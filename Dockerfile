# Etapa 1: Compilación
FROM eclipse-temurin:17-jdk AS builder
WORKDIR /app
COPY src ./src
RUN mkdir bin && javac src/com/bobds/simulador/*.java -d bin

# Etapa 2: Ejecución
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=builder /app/bin ./bin
EXPOSE 7777
ENTRYPOINT ["java", "-cp", "bin", "com.bobds.simulador.Main"]
