# 多阶段构建：server-boot 可执行 jar
# 国内拉不动 docker.io：Docker Desktop 配置阿里云 registry-mirrors（见 deploy/README 4.0）
# 用于 FROM 的 ARG 必须声明在第一个 FROM 之前（BuildKit 规则）
ARG MAVEN_IMAGE=maven:3.9.9-eclipse-temurin-21
ARG JRE_IMAGE=eclipse-temurin:21-jre-alpine

FROM ${MAVEN_IMAGE} AS build
WORKDIR /workspace

COPY pom.xml ./
COPY lib-core/pom.xml lib-core/pom.xml
COPY lib-app/pom.xml lib-app/pom.xml
COPY lib-app/app-kitchen/pom.xml lib-app/app-kitchen/pom.xml
COPY server-boot/pom.xml server-boot/pom.xml

RUN mvn -B -pl server-boot -am dependency:go-offline -DskipTests || true

COPY lib-core ./lib-core
COPY lib-app ./lib-app
COPY server-boot ./server-boot

RUN mvn -B -pl server-boot -am package -DskipTests \
    && cp server-boot/target/server-boot-*.jar /workspace/app.jar

FROM ${JRE_IMAGE} AS runtime
WORKDIR /app

RUN apk add --no-cache curl \
    && addgroup -S -g 1000 miyf \
    && adduser -S -u 1000 -G miyf miyf \
    && mkdir -p /miyf/log/server \
    && chown -R miyf:miyf /miyf/log

COPY --from=build /workspace/app.jar /app/app.jar
USER miyf

EXPOSE 8080

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom" \
    LOG_PATH=/miyf/log/server

HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=8 \
  CMD curl -fsS http://127.0.0.1:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
