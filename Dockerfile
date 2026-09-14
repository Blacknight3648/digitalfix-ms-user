# ==========================================
# Etapa 1: Build de la aplicación (Maven)
# ==========================================
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /app

# Copiar archivos de configuración de Maven para aprovechar la caché de capas
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copiar el código fuente y compilar omitiendo los tests para acelerar el build
COPY src ./src
RUN mvn clean package -DskipTests

# ==========================================
# Etapa 2: Imagen final de ejecución
# ==========================================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Crear un usuario no-root por seguridad
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copiar el artifact empaquetado desde la etapa de compilación
COPY --from=builder /app/target/*.jar app.jar

# Cambiar permisos al usuario sin privilegios
USER appuser

EXPOSE 8081

# Configuración de límites de memoria JVM
ENTRYPOINT ["java", "-XX:+UseG1GC", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]