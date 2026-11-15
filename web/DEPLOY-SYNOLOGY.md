# 部署到群晖 NAS

两种方式，**方式一最快**（和 `elec-score` 那种导入方式完全一致），方式二便于以后改站点。

先记住一条硬性前提：**iPad 要「装」这个应用，必须通过 https 访问**。
Safari 只在安全上下文（https 或 localhost）注册 Service Worker，否则图标能点开，但**断网就白屏**。
https 怎么配见后文。

---

## 方式一：导入镜像 tar（最快，推荐）

用的是 `schulte-table-1.1.0-amd64.tar`（61 MB）。这是 `docker load` 可直接吃的**镜像包**，
里面已经包含 nginx 和全部站点文件，**不需要在 NAS 上构建，也不需要上传源码**。

1. Container Manager → **映像** → **导入** → 从本地选择 `schulte-table-1.1.0-amd64.tar`
2. 等进度条走完，映像列表出现 **`schulte-table:1.1.0`**（约 160 MB）
3. 选中它 → **运行**，容器名填 `schulte-table`
4. **端口设置**：本地端口 `14174` → 容器端口 `80`（务必是 80）
5. 其余保持默认，启动
6. 浏览器打开 `http://NAS的IP:14174`，看到「舒尔特表 v1.1.0」即成功

> ⚠️ `schulte-table-docker.tar.gz` / `.zip` 是**源码包**，不是镜像，导入到「映像」里会报「格式无效」。
> 它给方式二用。

> 架构说明：本镜像为 **linux/amd64**，适用于 Intel/AMD 处理器的群晖（DS920+、DS1621+、DS923+ 等）。
> 如果你的是 ARM 机型（DS223、DS1522+ 之外的部分型号），需要重新打包，见文末。

---

## 方式二：compose 挂载部署（改站点方便）

这种不打包镜像，直接把 `site/` 挂进 nginx 容器。**改文件不用重建、不用重导镜像**，刷新即生效。

1. File Station 建目录 `/docker/schulte-table`，把 `schulte-table-docker.tar.gz` 传进去并解压，结构应为：

```
/docker/schulte-table
├── docker-compose.yml
├── nginx.conf
├── site/          ← index.html、app.js、styles.css、sw.js、manifest、icons
└── tools/         ← 重新打镜像包用的脚本
```

2. Container Manager → **项目** → **新增** → 路径选 `/docker/schulte-table`
3. 来源选「**使用现有的 docker-compose.yml**」→ 启动
4. 同样访问 `http://NAS的IP:14174`

端口冲突就改 `docker-compose.yml` 里的 `"14174:80"`（右边 80 是容器内端口，别动）。

---

## 配 https（iPad 装桌面图标的前提）

### 情况 A：有域名，且 NAS 能对外开 80/443

**反向代理**：控制面板 → 登录门户 → 高级 → 反向代理 → 新增

| 项 | 值 |
|---|---|
| 来源 协议 | HTTPS |
| 来源 主机名 | `schulte.你的域名.com` |
| 来源 端口 | 443 |
| 目的地 协议 | HTTP |
| 目的地 主机名 | `localhost` |
| 目的地 端口 | **14174** |

**证书**：控制面板 → 安全性 → 证书 → 新增 → 从 Let's Encrypt 获取 → 填该域名 →
然后在「设置」里把证书分配给上面这条反向代理。

前提：域名 A 记录指向你家公网 IP，且 80 端口可从外网访问（LE 验证用）。

### 情况 B：没有公网 IP（家宽常见）

Cloudflare Tunnel，免费且不用开端口：

1. 域名托管到 Cloudflare
2. 再加一个 `cloudflare/cloudflared` 容器，命令：
   `tunnel --no-autoupdate run --token <你的 Tunnel Token>`
3. Zero Trust 里建 Tunnel，Public Hostname 指向 `http://群晖内网IP:14174`

### 情况 C：只在内网

只能 `http://NAS的IP:14174`。能玩，但**没有离线能力**，属于网页不是应用。

---

## iPad 上安装

1. iPad 用 **Safari** 打开 https 地址（Chrome 装不了）
2. 等页面完全加载（首次要等 Service Worker 装好，约 1 秒）
3. 工具栏 **分享** → **添加到主屏幕** → 命名「舒尔特表」→ 添加
4. 从桌面图标启动，应为**全屏无地址栏**

**验证离线**：打开一次 → 开飞行模式 → 从桌面图标重进。能开始游戏、能看成绩，就对了。

---

## 以后更新站点

| 方式 | 更新方法 |
|---|---|
| 方式一（镜像） | 改完代码重新打镜像包 → 删除旧容器 → 删旧映像 → 重新导入 |
| 方式二（挂载） | 直接覆盖 `/docker/schulte-table/site/` 里的文件，**不用重启容器** |

缓存策略是 stale-while-revalidate：iPad 第一次打开还是旧版、后台已拉新版，**第二次打开就是新的**。
想立刻生效，就在 Safari 强刷一次，或删掉桌面图标重新添加。

---

## 重新打镜像包（改了站点之后）

需要本机能访问 Docker Hub。国内一般要走代理：

```bash
cd web
python3 tools/build_image_tar.py --proxy http://127.0.0.1:7890 --out ../schulte-table-1.1.0-amd64.tar
```

脚本会拉取 `nginx:alpine` 的 **linux/amd64** 层，追加一层站点文件，再按 `docker save` 格式重新打包。
基础层会缓存在 `/tmp/schulte-image-work/blobs`，重跑不会重复下载。

**ARM 机型**：改脚本里 `pick_amd64()` 为选 `arm64`，并把 `config['architecture']` 一起改成 `arm64`，
输出文件也建议改名以免混淆。

---

## 排查

| 现象 | 原因 |
|---|---|
| 导入镜像报「格式无效」 | 选成了源码包（`-docker.tar.gz` / `.zip`）。镜像包文件名是 `-amd64.tar` |
| 容器起来但页面 404 / 白屏 | 端口映射填错，容器端口必须是 80 |
| 装到桌面打开白屏 | 用的 http 访问，SW 没注册。换 https |
| 图标是灰色方块 | manifest 的 MIME 不对，确认走的是本项目 `nginx.conf` |
| 改完还是旧版 | SW 缓存。第二次打开，或删图标重装 |
| 断网打不开 | 首次加载没跑完就装了图标。联网打开等 2 秒再断网试 |
| 反向代理 502 | 容器没起，或目的地端口不是 14174 |
| ARM 群晖启动即退出 | 镜像架构不匹配，需按 arm64 重新打包 |
