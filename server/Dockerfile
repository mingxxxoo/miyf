# 多阶段构建：server-boot 可执行 jar
# 国内拉不动 docker.io：Docker Desktop 配置阿里云 registry-mirrors（见 deploy/README 4.0）
# Maven 依赖默认走 .mvn/settings.xml（阿里云镜像），避免 Central SSL 握手失败
# 用于 FROM 的 ARG 必须声明在第一个 FROM 之前（BuildKit 规则）
#
# 构建加速要点（需 Docker BuildKit，Desktop 默认已开）：
# 1) /root/.m2 cache mount：依赖跨构建复用，改业务代码不再重下 jar
# 2) 先只 COPY pom.xml 再 resolve：pom 未变时跳过依赖层
# 3) -T 1C：多模块并行编译
ARG MAVEN_IMAGE=maven:3.9.9-eclipse-temurin-21
ARG JRE_IMAGE=eclipse-temurin:21-jre-alpine

FROM ${MAVEN_IMAGE} AS build
WORKDIR /workspace

# ---- 1) 仅 POM + settings：依赖层可缓存 ----
COPY pom.xml ./
COPY .mvn ./.mvn

COPY lib-core/pom.xml ./lib-core/
COPY lib-core/core-common/pom.xml ./lib-core/core-common/
COPY lib-core/oss-service/pom.xml ./lib-core/oss-service/
COPY lib-core/notification-service/pom.xml ./lib-core/notification-service/
COPY lib-core/dictionary-service/pom.xml ./lib-core/dictionary-service/
COPY lib-core/config-service/pom.xml ./lib-core/config-service/
COPY lib-core/monitor-service/pom.xml ./lib-core/monitor-service/
COPY lib-core/job-service/pom.xml ./lib-core/job-service/
COPY lib-core/search-service/pom.xml ./lib-core/search-service/
COPY lib-core/workflow-service/pom.xml ./lib-core/workflow-service/

COPY lib-auth/pom.xml ./lib-auth/
COPY lib-auth/auth-service/pom.xml ./lib-auth/auth-service/
COPY lib-auth/user-service/pom.xml ./lib-auth/user-service/
COPY lib-auth/organization-service/pom.xml ./lib-auth/organization-service/
COPY lib-auth/permission-service/pom.xml ./lib-auth/permission-service/
COPY lib-auth/gateway-service/pom.xml ./lib-auth/gateway-service/

COPY lib-app/pom.xml ./lib-app/
COPY lib-app/kitchen-service/pom.xml ./lib-app/kitchen-service/
COPY lib-app/health-service/pom.xml ./lib-app/health-service/
COPY lib-app/ai-service/pom.xml ./lib-app/ai-service/

COPY server-boot/pom.xml ./server-boot/

# 预拉依赖（失败不阻断：个别插件在无源码时可能报错，真正 package 时仍会补齐）
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -T 1C -s .mvn/settings.xml -pl server-boot -am dependency:go-offline -DskipTests || true

# ---- 2) 源码变更才触发编译 ----
COPY lib-core ./lib-core
COPY lib-auth ./lib-auth
COPY lib-app ./lib-app
COPY server-boot ./server-boot

RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -T 1C -s .mvn/settings.xml -pl server-boot -am package -Dmaven.test.skip=true \
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
