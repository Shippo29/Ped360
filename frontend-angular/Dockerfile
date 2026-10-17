FROM node:22-alpine AS build
WORKDIR /app
COPY frontend-angular/package.json frontend-angular/package-lock.json ./
RUN npm ci
COPY frontend-angular/ .
RUN npm run build

FROM nginx:alpine
COPY frontend-angular/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist/frontend-angular/browser /usr/share/nginx/html
EXPOSE 80
HEALTHCHECK --interval=10s --timeout=5s --retries=12 CMD wget -q --spider http://127.0.0.1/ || exit 1
