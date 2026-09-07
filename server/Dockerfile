# 多阶段构建：server-boot 可执行 jar
# 国内拉不动 docker.io：Docker Desktop 配置阿里云 registry-mirrors（见 deploy/README 4.0）
# Maven 依赖默认走 .mvn/settings.xml（阿里云镜像），避免 Central SSL 握手失败
# 用于 FROM 的 ARG 必须声明在第一个 FROM 之前（BuildKit 规则）
ARG MAVEN_IMAGE=maven:3.9.9-eclipse-temurin-21
ARG JRE_IMAGE=eclipse-temurin:21-jre-alpine

FROM ${MAVEN_IMAGE} AS build
WORKDIR /workspace

COPY pom.xml ./
COPY .mvn ./.mvn
COPY lib-core ./lib-core
COPY lib-auth ./lib-auth
COPY lib-app ./lib-app
COPY server-boot ./server-boot

RUN mvn -B -s .mvn/settings.xml -pl server-boot -am package -Dmaven.test.skip=true \
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
