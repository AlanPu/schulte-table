#!/usr/bin/env python3
"""把静态站点打成一个 docker load 可直接导入的镜像 tar（无需本机 Docker）。

做法：从 Docker Hub 拉取 nginx:alpine 的 linux/amd64 层，追加一层放站点文件，
再按 docker save 的经典格式（manifest.json + <config>.json + <layer>/layer.tar）重新打包。

用法：
    python3 build_image_tar.py --proxy http://127.0.0.1:7890
"""

import argparse
import gzip
import hashlib
import io
import json
import os
import shutil
import subprocess
import sys
import tarfile
import time

REPO = 'library/nginx'
TAG = 'alpine'
REGISTRY = 'https://registry-1.docker.io'
AUTH = 'https://auth.docker.io/token'

INDEX_ACCEPT = ','.join([
    'application/vnd.oci.image.index.v1+json',
    'application/vnd.docker.distribution.manifest.list.v2+json',
])
MANIFEST_ACCEPT = ','.join([
    'application/vnd.oci.image.manifest.v1+json',
    'application/vnd.docker.distribution.manifest.v2+json',
])

# 脚本在 web/tools/ 下，站点与 nginx.conf 在上一级 web/
BASE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))


def log(msg):
    print(msg, flush=True)


def curl(args, retries=3):
    """调用 curl，失败重试。返回 bytes。"""
    last = None
    for attempt in range(retries):
        p = subprocess.run(args, capture_output=True)
        if p.returncode == 0 and p.stdout:
            return p.stdout
        last = p
        time.sleep(1.5 * (attempt + 1))
    raise RuntimeError(
        f'curl 失败: {args[-1][:90]}\n{p_stderr(last)}'
    )


def p_stderr(p):
    return (p.stderr or b'').decode('utf-8', 'replace')[:300]


class Client:
    def __init__(self, proxy=None):
        self.proxy = proxy
        self.token = None

    def _base(self):
        args = ['curl', '-sSL', '--max-time', '180', '--fail-with-body']
        if self.proxy:
            args += ['-x', self.proxy]
        return args

    def text(self, url, headers=None):
        args = self._base() + ['--max-time', '60', url]
        for h in (headers or []):
            args[1:1] = ['-H', h]
        return curl(args).decode('utf-8')

    def blob(self, url, headers=None):
        args = self._base() + [url]
        for h in (headers or []):
            args[1:1] = ['-H', h]
        return curl(args)

    def auth(self):
        data = json.loads(self.text(
            f'{AUTH}?service=registry.docker.io&scope=repository:{REPO}:pull'
        ))
        self.token = data['token']
        return self.token

    def h(self):
        if not self.token:
            self.auth()
        return [f'Authorization: Bearer {self.token}']


def sha256_hex(data):
    return hashlib.sha256(data).hexdigest()


def pick_amd64(index):
    for m in index.get('manifests', []):
        p = m.get('platform') or {}
        if p.get('os') == 'linux' and p.get('architecture') == 'amd64':
            return m['digest']
    raise RuntimeError('索引里没有 linux/amd64')


def add_dir(tf, name):
    ti = tarfile.TarInfo(name)
    ti.type = tarfile.DIRTYPE
    ti.mode = 0o755
    ti.uid = ti.gid = 0
    ti.uname = ti.gname = 'root'
    ti.mtime = 1700000000
    tf.addfile(ti)


def add_file(tf, arcname, data, mode=0o644):
    ti = tarfile.TarInfo(arcname)
    ti.size = len(data)
    ti.mode = mode
    ti.uid = ti.gid = 0
    ti.uname = ti.gname = 'root'
    ti.mtime = 1700000000
    tf.addfile(ti, io.BytesIO(data))


def build_new_layer(site_dir, nginx_conf):
    """生成包含站点与 nginx 配置的一层（未压缩 tar）。"""
    buf = io.BytesIO()
    with tarfile.open(fileobj=buf, mode='w', format=tarfile.GNU_FORMAT) as tf:
        for d in ['etc', 'etc/nginx', 'etc/nginx/conf.d',
                  'usr', 'usr/share', 'usr/share/nginx', 'usr/share/nginx/html',
                  'usr/share/nginx/html/icons']:
            add_dir(tf, d)

        with open(nginx_conf, 'rb') as f:
            add_file(tf, 'etc/nginx/conf.d/default.conf', f.read())

        for root, dirs, files in os.walk(site_dir):
            dirs.sort()
            for fn in sorted(files):
                if fn == '.DS_Store' or fn.startswith('._'):
                    continue
                full = os.path.join(root, fn)
                rel = os.path.relpath(full, site_dir).replace(os.sep, '/')
                with open(full, 'rb') as f:
                    add_file(tf, f'usr/share/nginx/html/{rel}', f.read())

    return buf.getvalue()


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--proxy', default=os.environ.get('HTTPS_PROXY', ''))
    ap.add_argument('--work', default='/tmp/schulte-image-work')
    ap.add_argument('--out', default='schulte-table-1.1.0-amd64.tar')
    ap.add_argument('--repo-tag', default='schulte-table:1.1.0')
    ap.add_argument('--site', default=os.path.join(BASE, 'site'))
    ap.add_argument('--nginx-conf', default=os.path.join(BASE, 'nginx.conf'))
    args = ap.parse_args()

    if not args.proxy:
        log('⚠️  没有代理，Docker Hub 可能不可达')

    client = Client(args.proxy or None)

    log(f'1/6 获取 {REPO}:{TAG} 的索引…')
    index = json.loads(client.text(
        f'{REGISTRY}/v2/{REPO}/manifests/{TAG}',
        [f'Accept: {INDEX_ACCEPT}'] + client.h()
    ))
    digest = pick_amd64(index)
    log(f'    选中 linux/amd64: {digest[:26]}…')

    log('2/6 获取 amd64 manifest…')
    manifest = json.loads(client.text(
        f'{REGISTRY}/v2/{REPO}/manifests/{digest}',
        [f'Accept: {MANIFEST_ACCEPT}'] + client.h()
    ))

    log('3/6 获取 config 与基础层…')
    config = json.loads(client.text(
        f"{REGISTRY}/v2/{REPO}/blobs/{manifest['config']['digest']}",
        client.h()
    ).encode('utf-8').decode('utf-8'))

    work = args.work
    blob_cache = f'{work}/blobs'
    os.makedirs(blob_cache, exist_ok=True)
    shutil.rmtree(f'{work}/layers', ignore_errors=True)
    os.makedirs(f'{work}/layers', exist_ok=True)

    def fetch(digest, is_blob=True):
        """下载并缓存 blob，重跑时不再重复消耗流量。"""
        key = digest.split(':')[-1]
        path = os.path.join(blob_cache, key)
        if os.path.exists(path):
            with open(path, 'rb') as f:
                return f.read()
        data = client.blob(f'{REGISTRY}/v2/{REPO}/blobs/{digest}', client.h())
        with open(path, 'wb') as f:
            f.write(data)
        return data

    diff_ids = config['rootfs']['diff_ids']
    if len(diff_ids) != len(manifest['layers']):
        raise RuntimeError('层数与 diff_ids 数量不一致')

    layer_shas = []
    for i, (layer, expected) in enumerate(zip(manifest['layers'], diff_ids)):
        blob = fetch(layer['digest'])
        mt = layer.get('mediaType', '')
        raw = gzip.decompress(blob) if ('gzip' in mt or blob[:2] == b'\x1f\x8b') else blob
        actual = 'sha256:' + sha256_hex(raw)
        if actual != expected:
            raise RuntimeError(f'第 {i} 层校验失败: {actual} != {expected}')
        path = f'{work}/layers/{sha256_hex(raw)}'
        with open(path, 'wb') as f:
            f.write(raw)
        layer_shas.append(sha256_hex(raw))
        log(f'    层 {i + 1}/{len(diff_ids)}  {len(raw) / 1048576:.1f} MB  ✓')

    log('4/6 生成站点层…')
    new_layer = build_new_layer(args.site, args.nginx_conf)
    new_sha = sha256_hex(new_layer)
    with open(f'{work}/layers/{new_sha}', 'wb') as f:
        f.write(new_layer)
    layer_shas.append(new_sha)
    log(f'    {len(new_layer) / 1024:.0f} KB')

    log('5/6 生成 config 与 manifest.json…')
    config['rootfs']['diff_ids'] = diff_ids + ['sha256:' + new_sha]
    config['history'] = config.get('history', []) + [{
        'created': time.strftime('%Y-%m-%dT%H:%M:%SZ', time.gmtime()),
        'created_by': 'COPY site/ /usr/share/nginx/html/ && COPY nginx.conf '
                      '/etc/nginx/conf.d/default.conf',
        'comment': '舒尔特表 PWA 静态站点',
    }]
    # 明确声明架构，避免在老 x86 群晖上被判定为不支持
    config['architecture'] = 'amd64'
    config['os'] = 'linux'

    config_bytes = json.dumps(config, separators=(',', ':'), sort_keys=True).encode('utf-8')
    config_sha = sha256_hex(config_bytes)

    manifest_out = [{
        'Config': f'{config_sha}.json',
        'RepoTags': [args.repo_tag],
        'Layers': [f'{s}/layer.tar' for s in layer_shas],
    }]

    log('6/6 打包…')
    out_path = os.path.abspath(args.out)
    with tarfile.open(out_path, 'w', format=tarfile.GNU_FORMAT) as tf:
        # config
        ti = tarfile.TarInfo(f'{config_sha}.json')
        ti.size = len(config_bytes)
        ti.mode = 0o644
        ti.mtime = int(time.time())
        tf.addfile(ti, io.BytesIO(config_bytes))

        # 各层
        for s in layer_shas:
            add_dir(tf, s)
            src = f'{work}/layers/{s}'
            ti = tarfile.TarInfo(f'{s}/layer.tar')
            ti.size = os.path.getsize(src)
            ti.mode = 0o644
            ti.mtime = int(time.time())
            with open(src, 'rb') as f:
                tf.addfile(ti, f)

        # manifest.json
        mbytes = json.dumps(manifest_out, separators=(',', ':')).encode('utf-8')
        ti = tarfile.TarInfo('manifest.json')
        ti.size = len(mbytes)
        ti.mode = 0o644
        ti.mtime = int(time.time())
        tf.addfile(ti, io.BytesIO(mbytes))

        # 老版本 docker load 会读这个
        v = b'1.0'
        ti = tarfile.TarInfo('VERSION')
        ti.size = len(v)
        ti.mode = 0o644
        ti.mtime = int(time.time())
        tf.addfile(ti, io.BytesIO(v))

    log('')
    log(f'✅ {out_path}  ({os.path.getsize(out_path) / 1048576:.1f} MB)')
    log(f'   层数 {len(layer_shas)}，镜像标签 {args.repo_tag}，架构 linux/amd64')


if __name__ == '__main__':
    sys.exit(main())
