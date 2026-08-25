# Этап сборки с Maven
FROM maven:4.0.0-rc-4-eclipse-temurin-21-alpine AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B -ntp
COPY src ./src
RUN mvn clean package -DskipTests -B -ntp

# Этап запуска
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Создаем пользователя и группу для приложения
ARG APP_USER=appuser
ARG APP_GROUP=appgroup
ARG APP_UID=1001
ARG APP_GID=1001

# Создаем группу и пользователя.
RUN addgroup -g ${APP_GID} -S ${APP_GROUP} && \
    adduser -u ${APP_UID} -S -G ${APP_GROUP} ${APP_USER}

# Копируем артефакт от builder-а и устанавливаем правильного владельца
COPY --from=builder --chown=${APP_USER}:${APP_GROUP} /app/target/*.jar app.jar

# Устанавливаем пользователя для запуска приложения
USER ${APP_USER}:${APP_GROUP}

# Приватная сеть Railway резолвится только в AAAA-записи, поэтому JVM должна
# предпочитать IPv6 при обращении к whisper/ai-service и managed БД.
ENV JAVA_TOOL_OPTIONS="-Djava.net.preferIPv6Addresses=true -XX:MaxRAMPercentage=60"

# Приложение слушает 8080 (server.port), а не 8090.
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
