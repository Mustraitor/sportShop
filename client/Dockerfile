# # 第一阶段：构建环境 (使用 Node.js)
# FROM node:16-alpine AS build-stage
# WORKDIR /app

# RUN apk add --no-cache git
# # 拷贝依赖配置并安装
# COPY package*.json ./
# # 如果下载慢，可以使用阿里镜像源：RUN npm install --registry=https://registry.npmmirror.com
# RUN npm install
# # 拷贝源码并打包
# COPY . .
# RUN npm run build:prod

# # 第二阶段：部署环境 (使用 Nginx)
# FROM nginx:stable-alpine
# # 【关键步】从 build-stage 拷贝生成的 dist 目录
# COPY --from=build-stage /app/dist /usr/share/nginx/html
# # 拷贝你的 nginx.conf 配置
# COPY nginx.conf /etc/nginx/conf.d/default.conf
# EXPOSE 80
# CMD ["nginx", "-g", "daemon off;"]




FROM nginx:stable-alpine
WORKDIR /usr/share/nginx/html
COPY dist/ /usr/share/nginx/html/
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
