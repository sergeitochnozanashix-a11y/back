# Деплой Learnizy

Три сервиса из трёх репозиториев, разворачиваются в один проект Railway.

## Топология

```
                    интернет
                        │
                        ▼  https, единственный публичный вход
              ┌───────────────────┐
              │      backend      │  :8080
              └─────────┬─────────┘
                        │  приватная сеть Railway (*.railway.internal, IPv6)
        ┌───────────┬───┴────┬───────────┬───────────┐
        ▼           ▼        ▼           ▼           ▼
    Postgres      Redis    minio      whisper       ai
    managed      managed   :9000      :8080       :8090
                          + volume   + volume
```

**Наружу смотрит только backend.** У `whisper` и `ai` нет никакой аутентификации:
публичный домен на них означает, что любой желающий сможет тратить ваш ключ
ProxyAPI и гонять транскрибацию за ваш счёт.

## Перед первым деплоем

### 1. Ротировать утёкшие секреты

Эти значения лежали в открытом виде в репозитории и остались в истории коммитов —
удаления из рабочей копии недостаточно, их нужно **заменить**:

| Что | Где лежало | Действие |
|---|---|---|
| Ключ ProxyAPI | `docker-compose.yml` | отозвать в кабинете ProxyAPI, выпустить новый |
| App Password Gmail | `application.yml`, `docker-compose.yml` | отозвать в Google Account → App passwords |
| Пароли Postgres / Redis / MinIO | `docker-compose.yml` | были только для локалки; в проде задать новые |
| `JWT_SECRET` | дефолт в `application.yml` | сгенерировать: `openssl rand -base64 48` |

### 2. Проверить аккаунт Railway

```bash
railway whoami
```

Репозитории приватные и лежат в организации `quinx-it` — у аккаунта Railway
должен быть доступ к ним через GitHub-интеграцию, иначе `--repo` в скрипте не
сработает (обходной путь — `railway up` из каталога каждого сервиса, но тогда
не будет автодеплоя по push).

## Разворачивание

```bash
cd learnizy-backend
JWT_SECRET="$(openssl rand -base64 48)" \
MINIO_ROOT_USER=learnizy \
MINIO_ROOT_PASSWORD="$(openssl rand -base64 18)" \
PROXYAPI_API_KEY="sk-новый-ключ" \
MAIL_USERNAME="…@gmail.com" \
MAIL_PASSWORD="новый app password" \
  bash deploy/railway-setup.sh
```

Скрипт создаёт проект, managed Postgres и Redis, три сервиса, два volume и
выставляет все переменные. Не идемпотентен — повторный запуск создаст второй
проект.

### Ручные шаги после скрипта

1. **MinIO не стартует без команды.** Дашборд → сервис `minio` → Settings →
   Deploy → Custom Start Command:
   ```
   server /data --console-address ":9001"
   ```
   CLI такое поле выставлять не умеет.

2. **Проверить имена managed-сервисов** (`railway status`). Переменные бэкенда
   ссылаются на `${{Postgres.*}}` и `${{Redis.*}}`; если сервисы назвались иначе,
   ссылки надо поправить.

3. **Прописать домен бэкенда во фронтенде.** Если домен не входит в
   `*.pxel.software`, добавить его в список origin'ов —
   [SecurityConfig.java:115-119](src/main/java/software/pxel/learneasy/config/security/SecurityConfig.java#L115-L119).

## Ключевые моменты конфигурации

**IPv6.** Приватная сеть Railway резолвится только в AAAA-записи, а JVM по
умолчанию предпочитает IPv4. Поэтому в каждом Dockerfile выставлен
`-Djava.net.preferIPv6Addresses=true`, а переменная `SERVER_ADDRESS=::`
заставляет Spring слушать IPv6 (на Linux такой bind принимает и IPv4). Без этого
backend не достучится ни до whisper, ни до ai, ни до баз.

**Порты фиксированные.** `PORT` задан явно (backend 8080, ai 8090, whisper 8080),
чтобы адреса вида `http://ai.railway.internal:8090` были предсказуемыми.

**DATABASE_URL от Railway не подходит Spring напрямую** — он в формате
`postgresql://user:pass@host/db`, а JDBC нужен `jdbc:postgresql://host:port/db`.
Поэтому `SPRING_DATASOURCE_URL` собирается из отдельных `PGHOST`/`PGPORT`/`PGDATABASE`.

**Volume у whisper обязателен и путь не выбирается.** `MODEL_DIR` захардкожен в
[WhisperCliClient.java:21](../learnizy-whisper-service/src/main/java/com/pxel/feedback/whisperserviceapp/component/WhisperCliClient.java#L21)
как `/home/app/.cache/whisper`. Без volume модель (~460 МБ) будет скачиваться
заново после каждого деплоя.

**Файлы отдаёт бэкенд, не MinIO.** `GET /api/v1/file-storage/**` открыт без
аутентификации, и whisper забирает аудио именно по этому публичному URL. MinIO
наружу выставлять не нужно.

## Автодеплой

Сервисы, созданные с `--repo`, Railway пересобирает сам при push в ветку по
умолчанию (`dev`). Отдельный CD-workflow в GitHub Actions не нужен —
существующие `ci.yml` продолжают гонять тесты и Sonar.

## Ресурсы и стоимость

| Сервис | RAM в простое | Заметки |
|---|---|---|
| backend | ~700 МБ | `MaxRAMPercentage=70` |
| ai | ~400 МБ | лёгкий фасад |
| whisper | ~400 МБ | +1.5 ГБ на время транскрибации моделью `small` |
| Postgres / Redis / minio | ~500 МБ суммарно | |

Ориентировочно $30-40/мес на Railway при небольшой нагрузке. Альтернатива —
один VPS 8 ГБ (Hetzner CPX41 ~€25/мес, Timeweb/Selectel сопоставимо) со всем
стеком через `docker-compose.yml` из этого репозитория; дешевле, но требует
самостоятельно поднимать nginx, TLS и бэкапы.

Если whisper упрётся в лимит размера образа или окажется дорогим, его одного
можно вынести на отдельный VPS — меняется только `WHISPER_API_URL` у бэкенда.

## Локальный запуск

```bash
cp .env.example .env    # заполнить
docker compose up --build -d                       # базовый стек
docker compose --profile ai --profile whisper up -d --build   # + AI и транскрибация
docker compose --profile monitoring up -d          # + Prometheus/Grafana/Loki
docker compose --profile tools up -d               # + pgAdmin
```

Профили комбинируются. `.env` в git не попадает.
