# Build and run Connto backend for AWS ECS (Fargate / EC2)
FROM maven:3.9.9-eclipse-temurin-17-alpine AS build
WORKDIR /src
COPY pom.xml .
COPY src ./src
RUN mvn -q -B package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S spring && adduser -S spring -G spring
COPY --from=build /src/target/*.jar app.jar
RUN chown spring:spring /app/app.jar
USER spring:spring
EXPOSE 8080
ENV JAVA_OPTS=""
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
