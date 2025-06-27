# 빌드 단계 (builder)
FROM openjdk:17-jdk-slim AS builder

# 1. 초기 작업 디렉토리 설정 및 전체 소스 복사
WORKDIR /app
COPY . /app

# 2. --- admin-service 빌드 ---
WORKDIR /app/admin-service
RUN chmod +x ./mvnw || { echo "ERROR: admin-service mvnw 스크립트를 찾을 수 없거나 권한이 없습니다. /app/admin-service 경로를 확인하세요."; exit 1; }
RUN ./mvnw dependency:go-offline -B
RUN ./mvnw package -DskipTests
ARG ADMIN_JAR_FILE_NAME=target/*.jar
RUN cp ${ADMIN_JAR_FILE_NAME} /app/admin-service-bundle.jar || { echo "Admin Service JAR 파일이 target 디렉토리에서 발견되지 않았습니다. 빌드 로그를 확인하세요."; exit 1;}


# 3. --- payment-service 빌드 ---
WORKDIR /app/payment-service
RUN chmod +x ./mvnw || { echo "ERROR: payment-service mvnw 스크립트를 찾을 수 없거나 권한이 없습니다. /app/payment-service 경로를 확인하세요."; exit 1; }
RUN ./mvnw dependency:go-offline -B
RUN ./mvnw package -DskipTests
ARG PAYMENT_JAR_FILE_NAME=target/*.jar
RUN cp ${PAYMENT_JAR_FILE_NAME} /app/payment-service-bundle.jar || { echo "Payment Service JAR 파일이 target 디렉토리에서 발견되지 않았습니다. 빌드 로그를 확인하세요."; exit 1;}


# 4. --- user-service 빌드 ---
WORKDIR /app/user-service
RUN chmod +x ./mvnw || { echo "ERROR: user-service mvnw 스크립트를 찾을 수 없거나 권한이 없습니다. /app/user-service 경로를 확인하세요."; exit 1; }
RUN ./mvnw dependency:go-offline -B
RUN ./mvnw package -DskipTests
ARG USER_JAR_FILE_NAME=target/*.jar
RUN cp ${USER_JAR_FILE_NAME} /app/user-service-bundle.jar || { echo "User Service JAR 파일이 target 디렉토리에서 발견되지 않았습니다. 빌드 로그를 확인하세요."; exit 1;}


# 실행 단계 (runtime)
FROM openjdk:17-slim
RUN useradd --system --uid 1000 spring
COPY entrypoint.sh /usr/local/bin/entrypoint.sh
RUN chmod +x /usr/local/bin/entrypoint.sh
USER spring
VOLUME /tmp
EXPOSE 8086
EXPOSE 8081
EXPOSE 8082
COPY --from=builder /app/admin-service-bundle.jar /app/admin-service.jar
COPY --from=builder /app/payment-service-bundle.jar /app/payment-service.jar
COPY --from=builder /app/user-service-bundle.jar /app/user-service.jar
ENTRYPOINT ["/usr/local/bin/entrypoint.sh"]
ENV SPRING_PROFILES_ACTIVE=production
