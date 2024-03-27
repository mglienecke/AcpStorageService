FROM --platform=linux/amd64 openjdk
# FROM openjdk
VOLUME /tmp
EXPOSE 8080
ENV ACP_STORAGE_CONNECTION_STRING=BlobEndpoint=https://acpstorage.blob.core.windows.net/;QueueEndpoint=https://acpstorage.queue.core.windows.net/;FileEndpoint=https://acpstorage.file.core.windows.net/;TableEndpoint=https://acpstorage.table.core.windows.net/;SharedAccessSignature=sv=2022-11-02&ss=b&srt=co&sp=rwdlaciytfx&se=2024-06-01T21:03:13Z&st=2024-02-17T14:03:13Z&spr=https&sig=k4fkwPI%2F%2BRPj0iNz2UAI5cjUfn%2FSuUMGVw6efEVQ9hQ%3D
ENV ACP_CONTAINER_NAME=acpcontainer


COPY target/AcpStorageService*.jar app.jar
ENTRYPOINT ["java", "-jar","/app.jar"]