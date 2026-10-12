# Stage 1: Build JARs with Java 25
FROM eclipse-temurin:25-jdk-noble AS build
WORKDIR /app

RUN apt-get update && apt-get install -y maven && rm -rf /var/lib/apt/lists/*

COPY pom.xml ./
COPY sensitive-data-service/pom.xml ./sensitive-data-service/
COPY api-gateway/pom.xml ./api-gateway/

COPY sensitive-data-service/src ./sensitive-data-service/src
COPY api-gateway/src ./api-gateway/src

RUN mvn clean package -pl api-gateway -am -DskipTests

# Stage 2: Runtime image with Java 25
FROM eclipse-temurin:25-jre-noble
WORKDIR /app

RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

COPY --from=build /app/api-gateway/target/api-gateway-*.jar app.jar
COPY api-gateway/src/main/resources/*.p12 /app/

EXPOSE 8443

ENTRYPOINT ["java", "-jar", "app.jar"]
