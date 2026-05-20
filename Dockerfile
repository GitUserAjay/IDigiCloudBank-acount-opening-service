# Use lightweight Java image
FROM eclipse-temurin:17-jdk

# Set working directory
WORKDIR /app

# Copy jar file
COPY target/bank-account-opening-service-1.0.0.jar app.jar

# Expose Render port
EXPOSE 8080

# Start application
ENTRYPOINT ["java","-jar","app.jar"]