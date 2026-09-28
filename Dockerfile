# syntax=docker/dockerfile:1.7
FROM eclipse-temurin:24-jdk-alpine AS build
WORKDIR /workspace

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw -B -ntp dependency:go-offline

COPY src/ src/
RUN ./mvnw -B -ntp -DskipTests package \
    && java -Djarmode=tools -jar target/rheinops-platform-*.jar extract --layers --destination target/extracted

FROM eclipse-temurin:24-jre-alpine AS runtime
RUN addgroup -S rheinops && adduser -S rheinops -G rheinops
WORKDIR /app

COPY --from=build --chown=rheinops:rheinops /workspace/target/extracted/dependencies/ ./
COPY --from=build --chown=rheinops:rheinops /workspace/target/extracted/spring-boot-loader/ ./
COPY --from=build --chown=rheinops:rheinops /workspace/target/extracted/snapshot-dependencies/ ./
COPY --from=build --chown=rheinops:rheinops /workspace/target/extracted/application/ ./

USER rheinops
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-Djava.security.egd=file:/dev/./urandom", "org.springframework.boot.loader.launch.JarLauncher"]
