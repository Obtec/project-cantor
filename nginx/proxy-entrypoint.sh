#!/bin/sh
# proxy(nginx) 기동 스크립트.
# - 진짜 인증서가 없으면 임시 더미 인증서를 만들어 nginx가 항상 부팅되도록 보장한다.
# - 백그라운드에서 진짜 인증서가 발급되면 즉시 reload, 이후 갱신 반영을 위해 주기적으로 reload.
# 이 스크립트는 compose의 `command` 로 실행된다(기본 entrypoint의 envsubst 처리는 그대로 동작).
set -eu

DOMAIN="${DOMAIN:?DOMAIN 환경변수가 필요합니다}"
LIVE="/etc/letsencrypt/live/$DOMAIN"

# 이미지 기본 default.conf 가 있으면 제거 — 우리 템플릿만 사용한다.
rm -f /etc/nginx/conf.d/default.conf

# nginx가 TLS 서버(443)를 띄우려면 인증서 파일이 존재해야 한다.
# 진짜 인증서가 아직 없으면 1일짜리 자가서명 더미를 만들어 부팅을 보장한다.
if [ ! -f "$LIVE/fullchain.pem" ]; then
    echo "[proxy] 임시 더미 인증서를 생성합니다(최초 1회)..."
    mkdir -p "$LIVE"
    apk add --no-cache openssl >/dev/null 2>&1 || true
    openssl req -x509 -nodes -newkey rsa:2048 -days 1 \
        -keyout "$LIVE/privkey.pem" -out "$LIVE/fullchain.pem" \
        -subj "/CN=$DOMAIN" >/dev/null 2>&1
    touch "$LIVE/.dummy"
fi

# 백그라운드 reload 워커.
(
    # 더미 상태라면, certbot이 진짜 인증서를 채울 때까지 기다렸다가 한 번 reload.
    if [ -f "$LIVE/.dummy" ]; then
        echo "[proxy] 진짜 인증서 발급을 대기합니다..."
        while [ -f "$LIVE/.dummy" ] || [ ! -f "$LIVE/fullchain.pem" ]; do
            sleep 5
        done
        echo "[proxy] 인증서 적용을 위해 nginx reload."
        nginx -s reload 2>/dev/null || true
    fi
    # 이후 갱신된 인증서를 주기적으로 반영.
    while :; do
        sleep 6h & wait $!
        nginx -s reload 2>/dev/null || true
    done
) &

exec nginx -g 'daemon off;'
