#!/bin/sh
# proxy(nginx) 기동 스크립트 — 인증서 없이도 항상 부팅된다.
# - 진짜 인증서가 없으면 HTTP 전용 설정으로 뜬다(ACME 챌린지 + 백엔드 프록시).
# - certbot이 진짜 인증서를 발급하면 자동으로 HTTPS 설정으로 전환하고 reload 한다.
# nginx가 존재하지 않는 인증서 파일을 참조하지 않으므로 크래시가 발생하지 않는다.
set -eu

DOMAIN="${DOMAIN:?DOMAIN 환경변수가 필요합니다}"
LIVE="/etc/letsencrypt/live/$DOMAIN"
CONF="/etc/nginx/conf.d/app.conf"

# 이미지 기본 default.conf 제거 — 우리 설정만 사용한다.
rm -f /etc/nginx/conf.d/default.conf

# HTTP 전용(인증서 발급 전): 챌린지를 서빙하고 나머지는 백엔드로 프록시한다.
write_http_only() {
    cat > "$CONF" <<EOF
# 로그인 무차별 대입 방지: IP당 분당 5회(+버스트 10)로 제한.
limit_req_zone \$binary_remote_addr zone=login:10m rate=5r/m;

server {
    listen 80;
    server_name $DOMAIN;

    location /.well-known/acme-challenge/ {
        root /var/www/certbot;
    }

    client_max_body_size 50m;

    location = /api/auth/login {
        limit_req zone=login burst=10 nodelay;
        resolver 127.0.0.11 valid=30s;
        set \$backend http://backend:8080;
        proxy_pass \$backend;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
    }

    location / {
        resolver 127.0.0.11 valid=30s;
        set \$backend http://backend:8080;
        proxy_pass \$backend;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
    }
}
EOF
}

# HTTP+HTTPS(인증서 발급 후): HTTP는 HTTPS로 리다이렉트, HTTPS에서 백엔드 프록시.
write_full() {
    cat > "$CONF" <<EOF
# 로그인 무차별 대입 방지: IP당 분당 5회(+버스트 10)로 제한.
limit_req_zone \$binary_remote_addr zone=login:10m rate=5r/m;

server {
    listen 80;
    server_name $DOMAIN;

    location /.well-known/acme-challenge/ {
        root /var/www/certbot;
    }

    location / {
        return 301 https://\$host\$request_uri;
    }
}

server {
    listen 443 ssl;
    http2 on;
    server_name $DOMAIN;

    ssl_certificate     $LIVE/fullchain.pem;
    ssl_certificate_key $LIVE/privkey.pem;

    # 보안 헤더
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header Referrer-Policy "strict-origin-when-cross-origin" always;

    client_max_body_size 50m;

    location = /api/auth/login {
        limit_req zone=login burst=10 nodelay;
        resolver 127.0.0.11 valid=30s;
        set \$backend http://backend:8080;
        proxy_pass \$backend;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
    }

    location / {
        resolver 127.0.0.11 valid=30s;
        set \$backend http://backend:8080;
        proxy_pass \$backend;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
    }
}
EOF
}

if [ -f "$LIVE/fullchain.pem" ]; then
    echo "[proxy] 기존 인증서 발견 → HTTPS 설정으로 기동합니다."
    write_full
else
    echo "[proxy] 인증서 없음 → HTTP 전용으로 기동합니다(certbot 발급 대기)."
    write_http_only
    # 백그라운드: 진짜 인증서가 들어오면 HTTPS 설정으로 전환하고 reload.
    (
        while [ ! -f "$LIVE/fullchain.pem" ]; do
            sleep 5
        done
        echo "[proxy] 인증서 발급 감지 → HTTPS 설정으로 전환하고 reload 합니다."
        write_full
        nginx -s reload 2>/dev/null || true
    ) &
fi

# 갱신된 인증서를 주기적으로 반영.
(
    while :; do
        sleep 6h & wait $!
        nginx -s reload 2>/dev/null || true
    done
) &

exec nginx -g 'daemon off;'
