# Build and run Connto backend for AWS ECS (Fargate / EC2).
# In ECS, set SPRING_PROFILES_ACTIVE=prod plus DB/JWT env (see application-prod.yml and infra/ecs/).
# Optional: JAVA_OPTS e.g. -Xms256m -Xmx768m (Fargate task memory must cover heap + metaspace + native).
FROM maven:3.9.9-eclipse-temurin-17-alpine AS build
WORKDIR /src
COPY pom.xml .
COPY src ./src
RUN mvn -q -B package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN apk add --no-cache curl \
    && addgroup -S spring && adduser -S spring -G spring
COPY --from=build /src/target/*.jar app.jar
RUN chown spring:spring /app/app.jar
USER spring:spring
EXPOSE 8080
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"
HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=3 \
    CMD curl -sf http://127.0.0.1:8080/actuator/health >/dev/null || exit 1
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
