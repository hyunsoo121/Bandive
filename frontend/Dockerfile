# syntax=docker/dockerfile:1
# 운영 배포용 이미지. 정적 빌드 산출물 + Caddy(TLS 종료·SPA 서빙·백엔드 리버스프록시)를 한 이미지로.
# GitHub Actions 에서 buildx 로 linux/arm64 (Graviton) 빌드.

# ── 빌드 스테이지 ──
FROM node:22-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
# Caddy 가 /api 등을 백엔드로 프록시하는 동일 오리진 배포라 빈 값(상대경로)으로 빌드.
ARG VITE_API_ORIGIN=""
ENV VITE_API_ORIGIN=$VITE_API_ORIGIN
RUN npm run build

# ── 런타임 스테이지 ──
FROM caddy:2-alpine
COPY Caddyfile /etc/caddy/Caddyfile
COPY --from=build /app/dist /srv
