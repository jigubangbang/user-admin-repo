# 빌드 단계 (builder)
FROM openjdk:17-jdk-slim AS builder
WORKDIR /app
COPY . /app
RUN chmod +x mvnw

# --- admin-service 빌드 ---
WORKDIR /app/admin-service
RUN ../mvnw dependency:go-offline -B
RUN ../mvnw package -DskipTests
ARG ADMIN_JAR_FILE_NAME=target/*.jar
RUN cp ${ADMIN_JAR_FILE_NAME} /app/admin-service.jar || { echo "Admin Service JAR not found"; exit 1;}

# --- payment-service 빌드 ---
WORKDIR /app/payment-service
RUN ../mvnw dependency:go-offline -B
RUN ../mvnw package -DskipTests
ARG PAYMENT_JAR_FILE_NAME=target/*.jar
RUN cp ${PAYMENT_JAR_FILE_NAME} /app/payment-service.jar || { echo "Payment Service JAR not found"; exit 1;}

# --- user-service 빌드 ---
WORKDIR /app/user-service
RUN ../mvnw dependency:go-offline -B
RUN ../mvnw package -DskipTests
ARG USER_JAR_FILE_NAME=target/*.jar
RUN cp ${USER_JAR_FILE_NAME} /app/user-service.jar || { echo "User Service JAR not found"; exit 1;}


# 실행 단계 (runtime)
FROM openjdk:17-slim
RUN useradd --system --uid 1000 spring
USER spring
VOLUME /tmp
EXPOSE 8086
EXPOSE 8081
EXPOSE 8082
COPY --from=builder /app/admin-service.jar /app/admin-service.jar
COPY --from=builder /app/payment-service.jar /app/payment-service.jar
COPY --from=builder /app/user-service.jar /app/user-service.jar
COPY entrypoint.sh /usr/local/bin/entrypoint.sh
RUN chmod +x /usr/local/bin/entrypoint.sh
ENTRYPOINT ["/usr/local/bin/entrypoint.sh"]
ENV SPRING_PROFILES_ACTIVE=production