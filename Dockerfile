# Imagen base con Java 21
FROM openjdk:21-jdk-slim

# Argumento: jar que genera Maven
ARG JAR_FILE=target/*.jar

# Copiar el jar compilado al contenedor
COPY ${JAR_FILE} app.jar

# Render asigna el puerto en $PORT
ENV PORT=8080
EXPOSE 8080

# Comando para arrancar la app
ENTRYPOINT ["java","-jar","/app.jar"]
