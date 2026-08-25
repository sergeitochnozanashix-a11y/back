# LearnEasy Backend

## Описание проекта

Этот проект представляет собой бэкенд для образовательной платформы. Он разработан с использованием Java 21, Spring
Boot, Spring Security и Spring Data JPA. В качестве базы данных используется PostgreSQL, для кэширования и временных
данных — Redis, а для хранения файлов (например, голосовых ответов) — MinIO (S3-совместимое хранилище).

Проект также включает **опциональную** интеграцию с:

* `ai-service` для расширения функциональности.
* **Стеком мониторинга и логирования** (Prometheus, Grafana, Loki) для отладки и анализа работы приложения.

Благодаря использованию профилей Docker Compose, эти сервисы можно запускать только при необходимости, не перегружая
основную сборку.

### Хранилища данных

* **PostgreSQL**: Является основной реляционной базой данных и хранит все ключевые, структурированные данные приложения.
    * **Пользователи**: Информация о пользователях, включая их роли и учётные данные (таблица `users`).
    * **Учебные материалы**: Структура курсов, модулей, уроков и их контентных блоков (таблицы `courses`, `modules`,
      `lessons`, `lesson_content_blocks`).
    * **Тестирование**: Информация о тестах, вопросах, попытках прохождения и ответах пользователей (таблицы `tests`,
      `test_question`, `test_attempts`, `test_answers`).
    * **Чаты**: Сообщения пользователей и ассистента, а также вложения к ним (таблицы `chats`, `chat_messages`,
      `message_attachments`).
    * **Статьи**: Информационные статьи или посты (таблица `articles`).

* **Redis**: Используется как быстрое in-memory key-value хранилище для временных и сессионных данных.
    * **Сессии и JWT**: Refresh-токены для аутентификации пользователей (префикс `refresh_token:`).
    * **Верификация и сброс пароля**: Временные коды для подтверждения почты и токены для сброса пароля (префиксы
      `verify:code:`, `password-reset:token:`).
    * **Ограничение запросов (Rate Limiting)**: Данные для контроля частоты запросов к API для защиты от флуда (префикс
      `rate-limit:user:`).

* **MinIO (S3)**: Выступает в роли объектного хранилища для неструктурированных данных (файлов).
    * **Голосовые файлы**: Голосовые ответы пользователей на тесты и аудиосообщения в чатах (бакет `voiceBucketName`, по
      умолчанию `voice-storage`).
    * **Медиафайлы**: Изображения, видео и другие файлы, используемые в контенте уроков или прикреплённые к сообщениям (
      бакет `mediaBucketName`, по умолчанию `media-storage`).

## Технологии

- Java 21
- Spring Boot 3.5.0+
- Spring Security (JWT)
- Spring Data JPA
- PostgreSQL
- MinIO (S3)
- Flyway (миграции БД)
- Maven (сборка проекта)
- Docker & Docker Compose (контейнеризация)
- Prometheus (сбор метрик)
- Grafana (визуализация данных и логов)
- Loki (агрегация логов)
- JUnit 5 (тестирование)
- Lombok
- MapStruct
- Springdoc OpenAPI

## Запуск проекта

### Требования

- JDK 21
- Maven 3.8+
- Docker
- Docker Compose

### Подготовка окружения

Секреты не хранятся в репозитории. Перед первым запуском создайте `.env`:

```bash
cp .env.example .env
```

и заполните пустые поля. `JWT_SECRET` удобно сгенерировать через
`openssl rand -base64 48`. Файл `.env` в git не попадает.

Без заполненных `POSTGRES_PASSWORD`, `REDIS_PASSWORD` и `JWT_SECRET` docker
compose откажется стартовать с явной ошибкой — это лучше, чем поднятое
приложение с пустым секретом.

### Запуск для разработки

Режимы управляются профилями Docker Compose и свободно комбинируются.

#### 1. Стандартный режим (только базовые сервисы)

Приложение, PostgreSQL, Redis и MinIO. Соседние репозитории не нужны.

```bash
docker compose up --build -d
```

#### 2. Запуск с мониторингом

Базовые сервисы **плюс** Prometheus, Grafana, Loki и Promtail.

```bash
docker compose --profile monitoring up --build -d
```

#### 3. Запуск с ProxyAPI-сервисом

Базовые сервисы **плюс** `ai-service`. Требует папку `../learnizy-proxyapi-service`
рядом с текущим проектом и заполненный `PROXYAPI_API_KEY` в `.env`.

```bash
docker compose --profile ai up --build -d
```

#### 4. Запуск с транскрибацией

Базовые сервисы **плюс** `whisper-service` (голосовые ответы и аудиосообщения).
Требует папку `../learnizy-whisper-service`. Первая сборка долгая — в образ
ставится torch и ffmpeg, а модель (~460 МБ) скачивается при первом запросе и
кэшируется в volume `whisper_cache`.

```bash
docker compose --profile whisper up --build -d
```

#### 5. pgAdmin

Вынесен в отдельный профиль, чтобы не занимать память при обычной разработке.

```bash
docker compose --profile tools up -d
```

#### 6. Всё включено

```bash
docker compose --profile ai --profile whisper --profile monitoring --profile tools up --build -d
```

### Доступные сервисы

* **Приложение**: [http://localhost:8080](http://localhost:8080)
* **Swagger UI**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
* **MinIO Console**: [http://localhost:9001](http://localhost:9001) — логин/пароль из `.env`
* **PGAdmin** (профиль `tools`): [http://localhost:5050](http://localhost:5050) — логин/пароль из `.env`
* **Whisper** (профиль `whisper`): [http://localhost:8085](http://localhost:8085)
* **ProxyAPI** (профиль `ai`): [http://localhost:8090](http://localhost:8090)
* **PostgreSQL**: порт `5433` на хосте
* **Redis**: порт `6379` на хосте
* **Grafana** (если активен профиль `monitoring`): [http://localhost:3000](http://localhost:3000) (логин/пароль:
  `admin`/`admin`)
* **Prometheus** (если активен профиль `monitoring`): [http://localhost:9090](http://localhost:9090)

### Остановка проекта

```bash
# Остановит и удалит все запущенные контейнеры, независимо от профиля
docker compose down
```

### Полезные команды при «тяжёлых» апдейтах

```bash
# Полная пересборка без кэша (только для основного приложения)
docker compose build app --no-cache && docker compose up -d app

# Если менялись зависимости/окружение всего стека
# (базовый запуск)
docker compose up -d --build

# (с мониторингом и AI)
docker compose --profile monitoring --profile ai up -d --build
```

### Запуск тестов

```bash
mvn test
```

## Структура проекта

- `DEPLOY.md` — развёртывание в проде (Railway) и ротация секретов.
- `deploy/railway-setup.sh` — одноразовое создание окружения в Railway.
- `.env.example` — шаблон локальных секретов.
- `.github/workflows/ci.yml` — конфигурация GitHub Actions CI.
- `prometheus/prometheus.yml` — конфигурация для Prometheus (сбор метрик с `app`).
- `grafana/provisioning/` — автоматическая настройка источников данных (Prometheus, Loki) для Grafana.
- `promtail/config.yml` — конфигурация для Promtail (сбор логов с Docker-контейнеров).
- `docker-compose.yml` — файл Docker Compose для локальной разработки.
- `pom.xml` — конфигурация Maven.

### Основной код (`src/main/java/software/pxel/learneasy`)

- `api` — DTO (Data Transfer Objects), используемые в API.
- `config` — конфигурация Spring (включая подпакет `security`).
- `constants` — константы приложения (маршруты, сообщения).
- `controller` — REST-контроллеры и их API-интерфейсы (в подпакете `api`).
- `exception` — классы исключений и глобальный обработчик.
- `feign` — клиенты для взаимодействия с внешними сервисами.
- `mapper` — MapStruct-мапперы для преобразования моделей в DTO.
- `model` — JPA-сущности и `enum`-ы.
- `repository` — Spring Data JPA репозитории.
- `service` — сервисный слой, содержащий бизнес-логику (включая подпакеты `async`, `util`).
- `LearneasyApplication.java` — входная точка приложения.

### Ресурсы (`src/main/resources`)

- `application.yml` — основной конфигурационный файл Spring Boot.
- `db/migration` — SQL-скрипты миграций Flyway.
- `templates` — HTML-шаблоны (например, для писем).

### Тесты (`src/test/java/software/pxel/learnizy`)

- `service/impl` — модульные и интеграционные тесты сервисов.
- прочие вспомогательные файлы для тестов.
