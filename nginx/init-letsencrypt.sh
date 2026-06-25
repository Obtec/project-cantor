#!/usr/bin/env bash
# Let's Encrypt 인증서 최초 발급 스크립트.
# 사용법: DOMAIN과 LETSENCRYPT_EMAIL 환경변수(또는 .env)를 설정한 뒤 실행.
#   ./nginx/init-letsencrypt.sh
#
# 사전 조건:
#   1) DOMAIN의 DNS A 레코드가 이 서버를 가리켜야 한다.
#   2) docker compose up -d 로 proxy(nginx)가 80포트에서 떠 있어야 한다.
set -euo pipefail

cd "$(dirname "$0")/.."

# .env 로드(있으면)
if [ -f .env ]; then
  set -a; . ./.env; set +a
fi

: "${DOMAIN:?DOMAIN 환경변수를 설정하세요 (예: journal.example.com)}"
: "${LETSENCRYPT_EMAIL:?LETSENCRYPT_EMAIL 환경변수를 설정하세요}"

STAGING=${STAGING:-0}
staging_arg=""
if [ "$STAGING" != "0" ]; then
  staging_arg="--staging"
  echo "[info] STAGING 모드로 발급합니다(테스트용)."
fi

echo "[info] ${DOMAIN} 인증서를 발급합니다..."

docker compose run --rm --entrypoint "\
  certbot certonly --webroot -w /var/www/certbot \
    $staging_arg \
    --email ${LETSENCRYPT_EMAIL} \
    --agree-tos --no-eff-email \
    -d ${DOMAIN}" certbot

echo "[info] 발급 완료. nginx/conf.d/app.conf 의 HTTPS server 블록 주석을 해제하고"
echo "       example.com 을 ${DOMAIN} 으로 바꾼 뒤 'docker compose restart proxy' 하세요."
