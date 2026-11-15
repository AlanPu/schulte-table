# 舒尔特表 PWA — 镜像构建（可选，用于分发或没装挂载的场景）
#
#   docker build -t schulte-table:1.1.0 .
#   docker run -d -p 14174:80 --name schulte-table schulte-table:1.1.0
#
# 群晖上更推荐用 docker-compose.yml 的挂载方式（改文件免重建镜像）。

FROM nginx:alpine

RUN rm -f /etc/nginx/conf.d/default.conf

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY site/     /usr/share/nginx/html/

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=3s --start-period=5s \
  CMD wget -qO- http://127.0.0.1/ >/dev/null 2>&1 || exit 1

# nginx 官方镜像默认前台运行，无需 CMD
