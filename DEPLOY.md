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

### 2. Подключить GitHub к Railway

**Это обязательный шаг, без него скрипт упадёт.** Railway не умеет линковать
репозиторий, пока к аккаунту не подключена GitHub-интеграция — и публичность
репозитория тут не помогает: `railway add --repo` отвечает
`Unauthorized. Please run railway login again.`, хотя логин при этом рабочий.

Дашборд → https://railway.com/account → Connect GitHub → выдать доступ к нужному
аккаунту или организации. Если репозитории лежат в чужой организации, установку
GitHub App должен одобрить её админ.

Обходной путь без GitHub — `railway up` из каталога каждого сервиса: код
заливается напрямую, но автодеплоя по push не будет.

### 3. Проверить аккаунт Railway

```bash
railway whoami
```

Проект создаётся в том аккаунте, под которым залогинен CLI. Скрипт печатает
аккаунт и ждёт подтверждения перед созданием чего-либо.

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

1. **MinIO разворачивается из `deploy/minio/Dockerfile`, а не из готового образа.**

   ```bash
   cd deploy/minio
   railway up -p <PROJECT_ID> -e production -s minio
   ```

   Поле **Custom Start Command у этого сервиса должно быть пустым.** Railway
   подменяет им `ENTRYPOINT` целиком, а не дописывает аргументы: значение
   `server /data ...` превращается в попытку запустить несуществующую программу
   `server`, и контейнер умирает мгновенно, не успев ничего написать в лог.
   Именно поэтому команда запечена в образ.

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
умолчанию репозитория. Отдельный CD-workflow в GitHub Actions не нужен —
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

## Грабли Railway CLI

Собрано на живом развёртывании, версия CLI 4.36.1:

* **`railway volume add --service <имя>` не работает.** Флаг `--service`
  относится к `railway volume`, а не к `add`, и принимает ID, а не имя — по имени
  CLI падает с `panicked at src/commands/volume.rs`. Рабочий вариант —
  `railway volume add -m <путь>` сразу после `railway add --service X`, который
  линкует созданный сервис.

* **Git Bash на Windows ломает пути томов.** `/data` превращается в
  `C:/Program Files/Git/data`, и CLI отвечает `Mount path must start with a /`.
  Лечится `export MSYS_NO_PATHCONV=1` — он уже стоит в скрипте.

* **`Unauthorized` при `--repo` — это про GitHub, а не про логин.** Никакой
  повторный `railway login` не помогает, нужна GitHub-интеграция (см. выше).

* **Managed-базы приносят свои тома сами.** Postgres и Redis создаются сразу с
  `postgres-volume` и `redis-volume`, вручную ничего добавлять не надо.

* **Custom Start Command заменяет ENTRYPOINT, а не CMD.** Для образа, у которого
  запуск задан через entrypoint (как `minio/minio`), это ломает контейнер без
  единой строки в логе. Надёжнее собрать свой образ с нужной командой и оставить
  поле пустым.

* **`railway logs` без аргумента показывает последний УСПЕШНЫЙ деплой**, а не
  последний. При разборе падения это уводит на устаревшие логи - передавайте ID
  деплоя явно:
  ```bash
  railway deployment list -s <service>
  railway logs -s <service> -d --lines 40 <DEPLOYMENT_ID>
  ```

* **`railway redeploy` переигрывает старый снимок вместе с его конфигом.** Чтобы
  применить изменённые настройки, нужен новый деплой (`railway up` или кнопка
  Deploy в дашборде).

* **Лимит бесплатного тарифа - 5 сервисов.** Шестой не создаётся:
  `Free plan resource provision limit exceeded`. Тома в этот лимит не входят -
  их удаление слот не освобождает.
