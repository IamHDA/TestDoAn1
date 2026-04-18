# Stage 1: Build với Maven
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /backend

# copy pom.xml trước để cache dependencies
COPY pom.xml .
RUN mvn dependency:go-offline

# copy source code và build
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Runtime với JDK nhẹ
FROM eclipse-temurin:17-jdk-alpine
WORKDIR /backend


# Tạo thư mục logs và cấp quyền
RUN mkdir -p /app/logs && chmod 755 /app/logs

# copy jar từ stage build
COPY --from=builder /backend/target/*.jar app.jar
EXPOSE 8081
ENV SPRING_PROFILES_ACTIVE=prod
ENV LOG_DIR=/app/logs
ENV TZ=Asia/Ho_Chi_Minh
ENTRYPOINT ["java","-Duser.timezone=Asia/Ho_Chi_Minh","-jar","app.jar"]