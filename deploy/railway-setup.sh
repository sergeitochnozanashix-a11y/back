#!/usr/bin/env bash
#
# Одноразовое создание окружения Learnizy в Railway.
# Запускать из каталога, где лежат все три репозитория рядом:
#
#   herf/
#     learnizy-backend/          <- отсюда запускается скрипт: deploy/railway-setup.sh
#     learnizy-proxyapi-service/
#     learnizy-whisper-service/
#
# Перед запуском:
#   1. railway whoami            - убедиться, что это нужный аккаунт
#   2. заполнить секции SECRETS ниже
#
# Скрипт не идемпотентен: повторный запуск создаст второй проект.
# Подробности и ручные шаги - в DEPLOY.md.
set -euo pipefail

# Git Bash на Windows подменяет абсолютные пути вида /data на C:/Program Files/...
# Без этого railway volume add ругается 'Mount path must start with a /'.
export MSYS_NO_PATHCONV=1

PROJECT_NAME="${PROJECT_NAME:-learnizy}"

# ------------------------------ SECRETS -------------------------------------
# Заполнить перед запуском. Скрипт откажется работать с пустыми значениями.
JWT_SECRET="${JWT_SECRET:-}"                 # openssl rand -base64 48
MINIO_ROOT_USER="${MINIO_ROOT_USER:-}"
MINIO_ROOT_PASSWORD="${MINIO_ROOT_PASSWORD:-}"
PROXYAPI_API_KEY="${PROXYAPI_API_KEY:-}"     # НОВЫЙ ключ: старый утёк в git
MAIL_USERNAME="${MAIL_USERNAME:-}"
MAIL_PASSWORD="${MAIL_PASSWORD:-}"           # для Gmail - App Password
FRONTEND_RESET_PASSWORD_URL="${FRONTEND_RESET_PASSWORD_URL:-https://edu.pxel.software/reset-password}"
PROXYAPI_BASE_URL="${PROXYAPI_BASE_URL:-https://api.proxyapi.ru/openai/v1}"
# ----------------------------------------------------------------------------

for var in JWT_SECRET MINIO_ROOT_USER MINIO_ROOT_PASSWORD PROXYAPI_API_KEY \
           MAIL_USERNAME MAIL_PASSWORD; do
  if [ -z "${!var}" ]; then
    echo "ОШИБКА: не задан $var (см. секцию SECRETS в начале скрипта)" >&2
    exit 1
  fi
done

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
WORKDIR="$(dirname "$REPO_ROOT")"

for dir in learnizy-backend learnizy-proxyapi-service learnizy-whisper-service; do
  if [ ! -d "$WORKDIR/$dir" ]; then
    echo "ОШИБКА: не найден $WORKDIR/$dir - все три репозитория должны лежать рядом" >&2
    exit 1
  fi
done

echo "==> Аккаунт Railway:"
railway whoami

# Проект создаётся в том аккаунте, под которым залогинен CLI. Ошибиться тут
# дорого: ресурсы уедут не туда, и найти их потом непросто.
if [ -z "${SKIP_ACCOUNT_CONFIRM:-}" ]; then
  read -r -p "Разворачиваем в этом аккаунте? [y/N] " ans </dev/tty
  case "$ans" in
    [yY]*) ;;
    *) echo "Отменено. Смените аккаунт: railway logout && railway login" >&2; exit 1 ;;
  esac
fi

echo "==> Создание проекта $PROJECT_NAME"
cd "$REPO_ROOT"
railway init -n "$PROJECT_NAME"

echo "==> Managed-базы"
railway add --database postgres
railway add --database redis


# --------------------------------- MinIO ------------------------------------
# ВНИМАНИЕ: образ minio/minio не стартует без аргументов, а CLI не умеет
# задавать Custom Start Command. После этого шага откройте сервис minio в
# дашборде -> Settings -> Deploy -> Custom Start Command и впишите:
#
#   server /data --console-address ":9001"
#
echo "==> MinIO"
railway add --service minio --image minio/minio \
  --variables "MINIO_ROOT_USER=$MINIO_ROOT_USER" \
  --variables "MINIO_ROOT_PASSWORD=$MINIO_ROOT_PASSWORD"
railway volume add -m /data   # том цепляется к залинкованному сервису (minio)

# JAVA_TOOL_OPTIONS ниже перекрывает значение из Dockerfile: там стоит
# MaxRAMPercentage (доля от лимита контейнера), а на Railway контейнер видит
# лимит тарифа (8+ ГБ) - JVM раздула бы хип, а платить пришлось бы за
# фактически занятую память. Поэтому здесь фиксированный -Xmx.

# ------------------------------- Whisper ------------------------------------
# MODEL_DIR захардкожен в WhisperCliClient, поэтому mount-path обязан совпадать.
echo "==> Whisper"
railway add --service whisper --repo quinx-it/learnizy-whisper-service \
  --variables "JAVA_TOOL_OPTIONS=-Djava.net.preferIPv6Addresses=true -Xmx384m" \
  --variables "PORT=8080" \
  --variables "SERVER_ADDRESS=::" \
  --variables "WHISPER_MODEL_NAME=small" \
  --variables "WHISPER_MAX_DOWNLOAD_MB=50"
railway volume add -m /home/app/.cache/whisper   # к залинкованному whisper

# ------------------------------- ProxyAPI -----------------------------------
echo "==> ProxyAPI (ai)"
railway add --service ai --repo quinx-it/learnizy-proxyapi-service \
  --variables "JAVA_TOOL_OPTIONS=-Djava.net.preferIPv6Addresses=true -Xmx384m" \
  --variables "PORT=8090" \
  --variables "SERVER_ADDRESS=::" \
  --variables "PROXYAPI_BASE_URL=$PROXYAPI_BASE_URL" \
  --variables "PROXYAPI_API_KEY=$PROXYAPI_API_KEY"

# -------------------------------- Backend -----------------------------------
# Ссылки ${{Service.VAR}} резолвит сам Railway, поэтому одинарные кавычки.
echo "==> Backend"
railway add --service backend --repo quinx-it/learnizy-backend \
  --variables "JAVA_TOOL_OPTIONS=-Djava.net.preferIPv6Addresses=true -Xmx768m -XX:MaxMetaspaceSize=256m" \
  --variables "PORT=8080" \
  --variables "SERVER_ADDRESS=::" \
  --variables 'SPRING_DATASOURCE_URL=jdbc:postgresql://${{Postgres.PGHOST}}:${{Postgres.PGPORT}}/${{Postgres.PGDATABASE}}' \
  --variables 'SPRING_DATASOURCE_USERNAME=${{Postgres.PGUSER}}' \
  --variables 'SPRING_DATASOURCE_PASSWORD=${{Postgres.PGPASSWORD}}' \
  --variables "SPRING_JPA_HIBERNATE_DDL_AUTO=validate" \
  --variables 'SPRING_DATA_REDIS_HOST=${{Redis.REDISHOST}}' \
  --variables 'SPRING_DATA_REDIS_PORT=${{Redis.REDISPORT}}' \
  --variables 'SPRING_DATA_REDIS_PASSWORD=${{Redis.REDISPASSWORD}}' \
  --variables "SPRING_CACHE_TYPE=redis" \
  --variables "JWT_SECRET=$JWT_SECRET" \
  --variables "JWT_EXPIRATION_ACCESS=3600000" \
  --variables "JWT_EXPIRATION_REFRESH=86400000" \
  --variables 'MINIO_URL=http://${{minio.RAILWAY_PRIVATE_DOMAIN}}:9000' \
  --variables 'MINIO_ACCESS_KEY=${{minio.MINIO_ROOT_USER}}' \
  --variables 'MINIO_SECRET_KEY=${{minio.MINIO_ROOT_PASSWORD}}' \
  --variables "MINIO_VOICE_BUCKET_NAME=voice-responses" \
  --variables "MINIO_MEDIA_BUCKET_NAME=media-files" \
  --variables "MINIO_REGION=us-east-1" \
  --variables "MAIL_HOST=smtp.gmail.com" \
  --variables "MAIL_PORT=587" \
  --variables "MAIL_USERNAME=$MAIL_USERNAME" \
  --variables "MAIL_PASSWORD=$MAIL_PASSWORD" \
  --variables "FRONTEND_RESET_PASSWORD_URL=$FRONTEND_RESET_PASSWORD_URL" \
  --variables "PASSWORD_RESET_TOKEN_EXPIRY_MINUTES=60" \
  --variables 'WHISPER_API_URL=http://${{whisper.RAILWAY_PRIVATE_DOMAIN}}:8080' \
  --variables 'AI_API_URL=http://${{ai.RAILWAY_PRIVATE_DOMAIN}}:8090'

# Публичный домен только у бэкенда. whisper, ai и minio остаются во внутренней
# сети: у них нет никакой аутентификации.
echo "==> Публичный домен для backend"
railway domain --service backend --port 8080

cat <<'DONE'

Готово. Что осталось сделать руками:

  1. minio -> Settings -> Deploy -> Custom Start Command:
         server /data --console-address ":9001"
     без этого контейнер не стартует.

  2. Проверить, что managed-сервисы называются именно Postgres и Redis
     (ссылки ${{Postgres.*}} / ${{Redis.*}} в переменных backend завязаны
     на эти имена):
         railway status

  3. Первый деплой whisper долгий: собирается образ с torch,
     плюс модель ~460 МБ скачивается при первом запросе.

  4. Прописать домен бэкенда во фронтенде и, если он не на *.pxel.software,
     добавить origin в SecurityConfig.corsConfigurationSource.

DONE
