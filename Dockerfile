#
# Build stage
#
# Use an official Maven image as the base image
FROM maven:3.9.9-amazoncorretto-21-debian AS build
# Set the working directory in the container
WORKDIR /app
# Copy the pom.xml and the project files to the container
COPY pom.xml .
COPY src ./src
# Build the application using Maven
RUN mvn clean package -DskipTests
# Use an official OpenJDK image as the base image
FROM openjdk:21
# Set the working directory in the container
WORKDIR /app
# Copy the built JAR file from the previous stage to the container
COPY --from=build /app/target/AcpStorageService*.jar app.jar
EXPOSE 8080

ENV ACP_STORAGE_CONNECTION=BlobEndpoint=https://acpstorage.blob.core.windows.net/acpcontainer?sp=racwdli&st=2025-03-06T19:01:08Z&se=2025-05-01T02:01:08Z&spr=https&sv=2022-11-02&sr=c&sig=cVrFc2NcsOZ3U%2BITpMd1T%2BVMSEOBCAekpZhYQEIrd20%3D
ENV ACP_BLOB_CONTAINER=acpcontainer
ENV USE_REDIS=false

# Set the command to run the application
CMD ["java", "-jar", "./app.jar"]