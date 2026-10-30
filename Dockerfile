
FROM eclipse-temurin:21-jre

WORKDIR /app

COPY target/seat-inventory-service-1.0.0.jar seat-inventory-service.jar

EXPOSE 8889

ENTRYPOINT ["java", "-jar", "seat-inventory-service.jar"]