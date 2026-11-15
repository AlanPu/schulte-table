/* 舒尔特表 PWA — Service Worker
   缓存策略：
   · 安装时预缓存全部资源，保证首次加载后能完全离线
   · 运行期用 stale-while-revalidate：先秒开缓存，同时后台拉新版本
     —— 这样改了代码用户下次打开就是新的，不会一直卡在旧版
*/

const VERSION = 'v1.1.2';
const CACHE = `schulte-${VERSION}`;

const ASSETS = [
  './',
  './index.html',
  './styles.css',
  './app.js',
  './manifest.webmanifest',
  './icons/icon-180.png',
  './icons/icon-192.png',
  './icons/icon-512.png',
  './icons/icon-512-maskable.png',
  './icons/icon-1024.png'
];

self.addEventListener('install', (event) => {
  event.waitUntil(
    caches.open(CACHE)
      .then((cache) => cache.addAll(ASSETS))
      .then(() => self.skipWaiting())
  );
});

self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches.keys()
      .then((keys) => Promise.all(
        keys.filter((k) => k !== CACHE).map((k) => caches.delete(k))
      ))
      .then(() => self.clients.claim())
  );
});

self.addEventListener('fetch', (event) => {
  const req = event.request;
  if (req.method !== 'GET') return;

  const url = new URL(req.url);
  if (url.origin !== self.location.origin) return;

  // 页面导航统一回落到 index.html（单页应用），其余按真实请求匹配
  const key = req.mode === 'navigate' ? './index.html' : req;

  event.respondWith(
    caches.open(CACHE).then(async (cache) => {
      const cached = await cache.match(key);

      const network = fetch(req).then((res) => {
        if (res && res.status === 200 && res.type === 'basic') {
          cache.put(key, res.clone());
        }
        return res;
      }).catch(() => null);

      if (cached) {
        // 有缓存：立刻返回，网络结果顺手更新缓存
        event.waitUntil(network);
        return cached;
      }

      const fresh = await network;
      return fresh || new Response('离线且无缓存', {
        status: 503,
        headers: { 'Content-Type': 'text/plain; charset=utf-8' }
      });
    })
  );
});
