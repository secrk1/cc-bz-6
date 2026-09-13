# -------- 构建阶段 --------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

# 先拷贝 pom 预下载依赖，利用 Docker 层缓存
COPY pom.xml .
RUN mvn -B dependency:go-offline -DskipTests

COPY src ./src
RUN mvn -B clean package -DskipTests

# -------- 运行阶段 --------
FROM eclipse-temurin:17-jre
WORKDIR /app

ENV TZ=Asia/Shanghai
COPY --from=build /build/target/workflow-approval.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
