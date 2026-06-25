#!/bin/sh
# certbot 기동 스크립트.
# - 진짜 인증서가 없으면 proxy(80)가 뜨길 기다렸다가 자동 발급한다(webroot/HTTP-01).
# - 이후 12시간마다 갱신을 시도한다.
set -eu

DOMAIN="${DOMAIN:?DOMAIN 환경변수가 필요합니다}"
EMAIL="${LETSENCRYPT_EMAIL:?LETSENCRYPT_EMAIL 환경변수가 필요합니다}"
LIVE="/etc/letsencrypt/live/$DOMAIN"

staging_arg=""
if [ "${STAGING:-0}" != "0" ]; then
    staging_arg="--staging"
    echo "[certbot] STAGING 모드(테스트용)로 발급합니다."
fi

# 진짜 인증서가 아직 없거나(또는 proxy가 만든 더미만 있으면) 발급한다.
if [ ! -f "$LIVE/fullchain.pem" ] || [ -f "$LIVE/.dummy" ]; then
    echo "[certbot] proxy(80) 연결을 기다립니다..."
    until python3 -c "import socket; socket.create_connection(('proxy', 80), 3)" 2>/dev/null; do
        sleep 3
    done

    echo "[certbot] ${DOMAIN} 인증서를 발급합니다..."
    rm -rf "$LIVE"   # proxy가 만든 더미 제거 후 깨끗하게 발급
    if ! certbot certonly --webroot -w /var/www/certbot $staging_arg \
            --email "$EMAIL" --agree-tos --no-eff-email --non-interactive \
            -d "$DOMAIN"; then
        echo "[certbot] 발급 실패. 레이트리밋 보호를 위해 10분 후 재시도합니다."
        echo "          (DNS A레코드 / 공유기 80,443 포트포워딩 / 방화벽을 확인하세요.)"
        sleep 600
        exit 1   # restart 정책에 의해 재시도
    fi
    echo "[certbot] 발급 완료."
fi

# 자동 갱신 루프.
trap exit TERM
while :; do
    certbot renew --webroot -w /var/www/certbot
    sleep 12h & wait $!
done
