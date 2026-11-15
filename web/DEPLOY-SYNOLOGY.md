# 部署到群晖 NAS（Docker）

iPad 上要「安装」这个应用，只有一条硬性前提：**必须通过 https 访问**。
Safari 只在安全上下文（https 或 localhost）才允许注册 Service Worker，否则装出来的图标能点开，但**没有离线能力**，断网就是白屏。

以下按群晖 DSM 7.2 / Container Manager 写。

---

## 一、上传并启动（挂载式，推荐）

这种方式改文件不用重建镜像。

1. File Station 里建目录 `/docker/schulte-table`，把压缩包解压进去，结构应为：

```
/docker/schulte-table
├── docker-compose.yml
├── nginx.conf
└── site/
    ├── index.html
    ├── app.js
    ├── styles.css
    ├── manifest.webmanifest
    ├── sw.js
    └── icons/…
```

2. Container Manager → **项目** → **新增**
3. 项目名称填 `schulte-table`，路径选 `/docker/schulte-table`
4. 来源选「**使用现有的 docker-compose.yml**」，下一步，启动
5. 浏览器打开 `http://NAS的IP:14174`，能看到「舒尔特表 v1.1.0」就通了

> 14174 若被占用，把 `docker-compose.yml` 里的 `"14174:80"` 换成别的宿主端口（右边 `80` 是容器内 nginx，别动），后续访问地址同步改。

---

## 二、配 https（关键一步）

### 情况 A：有域名，且 NAS 能对外开 80/443

**1. 反向代理**

控制面板 → **登录门户** → **高级** → **反向代理** → 新增：

| 项 | 值 |
|---|---|
| 来源 协议 | HTTPS |
| 来源 主机名 | `schulte.你的域名.com` |
| 来源 端口 | 443 |
| 目的地 协议 | HTTP |
| 目的地 主机名 | `localhost` |
| 目的地 端口 | 14174 |

**2. 证书**

控制面板 → **安全性** → **证书** → 新增 → **从 Let's Encrypt 获取** → 域名填 `schulte.你的域名.com`。
拿到后点「设置」→ 把该证书分配给上面这条反向代理。

前提：域名 A 记录已指向你家公网 IP，且 80 端口可从外网访问（Let's Encrypt 验证用）。

### 情况 B：没有公网 IP（家宽常见）

用 Cloudflare Tunnel，免费，且不用开任何端口：

1. 域名托管到 Cloudflare
2. 在 Container Manager 里再加一个 `cloudflare/cloudflared` 容器，命令：
   `tunnel --no-autoupdate run --token <你的 Tunnel Token>`
3. Cloudflare Zero Trust 里建 Tunnel，Public Hostname 指向 `http://群晖内网IP:14174`

得到的 `https://schulte.你的域名.com` 就是带证书的正式地址。

### 情况 C：只在内网用

那就只能用 `http://NAS的IP:14174`。**能玩，但装出来没有离线能力**，属于网页而不是应用。想离线必须走 A 或 B。

---

## 三、iPad 上安装

1. iPad 用 **Safari** 打开你的 https 地址（必须 Safari，Chrome 装不了）
2. 等页面完全加载（第一次要等 Service Worker 装好，约 1 秒）
3. 点工具栏 **分享** 图标 → **添加到主屏幕**
4. 名字改成「舒尔特表」→ **添加**
5. 从桌面图标启动，应该是**全屏无地址栏**，安全区（刘海/圆角）不被遮挡

**验证离线**：打开一次 → 打开飞行模式 → 从桌面图标重新进入。能正常开始游戏、能看成绩记录，就说明装对了。

---

## 四、以后更新

挂载式部署下，改 `/docker/schulte-table/site/` 里的文件即可，**不用重启容器**。

缓存策略是 stale-while-revalidate：iPad 第一次打开还是旧版、后台已经拉好新版，**第二次打开就是新的**。
想立刻见效，就在 Safari 里强制刷新一次，或删掉桌面图标重新添加。

> 改了 `sw.js` 里的 `VERSION` 会触发整体缓存重建，适合发布不兼容的大改动。

---

## 五、可选：构建成镜像再部署

不想挂载目录、或者要把镜像发给别人：

```bash
docker build -t schulte-table:1.1.0 .
docker run -d -p 14174:80 --name schulte-table schulte-table:1.1.0
```

或在本目录执行：

```bash
docker compose -f docker-compose.build.yml up -d --build
```

注意这种方式改文件后必须重新 build。

---

## 六、排查

| 现象 | 原因 |
|---|---|
| 装到桌面打开是白屏 | 用了 http 访问，Service Worker 没注册。换成 https |
| 图标是灰色方块/截图 | `manifest.webmanifest` 返回的 MIME 不对，检查 nginx 是否返回 `application/manifest+json` |
| 改完还是旧版 | SW 缓存。等第二次打开，或删图标重装 |
| 断网后打不开 | 首次加载没跑完就装了图标。联网打开一次、等 2 秒，再断网试 |
| 反向代理 502 | 容器没起，或目的地端口不是容器实际端口 |
