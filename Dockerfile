# syntax=docker/dockerfile:1

# Etapa 1: compila o projeto e gera o JAR.
FROM maven:3.9.11-eclipse-temurin-17 AS build

WORKDIR /workspace

# Copiar o POM antes do codigo permite reutilizar o cache das dependencias.
COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline

COPY src ./src
RUN mvn -q -DskipTests package

# Etapa 2: imagem final apenas com o Java e a aplicacao compilada.
FROM eclipse-temurin:17-jre-jammy AS runtime

WORKDIR /app

# A aplicacao nao precisa executar como root.
RUN groupadd --system spring && useradd --system --gid spring spring

COPY --from=build --chown=spring:spring /workspace/target/*.jar app.jar

USER spring:spring

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
