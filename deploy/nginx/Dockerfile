# 构建管理后台静态资源并打入 Nginx 镜像
# 国内拉不动 docker.io：配置阿里云 registry-mirrors（见 deploy/README 4.0）
# 用于 FROM 的 ARG 必须声明在第一个 FROM 之前（BuildKit 规则）
ARG NODE_IMAGE=node:20-alpine
ARG NGINX_IMAGE=nginx:1.27-alpine

FROM ${NODE_IMAGE} AS admin-build
WORKDIR /web

COPY miyf-admin/package.json miyf-admin/package-lock.json* ./
RUN if [ -f package-lock.json ]; then npm ci; else npm install; fi

COPY miyf-admin/ ./
RUN npm run build

FROM ${NGINX_IMAGE} AS runtime

COPY deploy/nginx/nginx.conf /etc/nginx/nginx.conf
COPY deploy/nginx/conf.d/default.conf /etc/nginx/conf.d/default.conf
COPY --from=admin-build /web/dist /usr/share/nginx/html

# HTTPS 模板仅作参考，勿放入 /etc/nginx/templates（官方镜像会 envsubst 进 conf.d）
COPY deploy/nginx/conf.d/https.conf.template /usr/share/nginx/https.conf.template

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
  CMD wget -qO- http://127.0.0.1/healthz || exit 1
