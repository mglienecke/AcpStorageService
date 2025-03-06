FROM openjdk:21
# FROM openjdk
VOLUME /tmp
EXPOSE 8080
ENV ACP_STORAGE_CONNECTION=BlobEndpoint=https://acpstorage.blob.core.windows.net/acpcontainer?sp=racwdli&st=2025-03-06T19:01:08Z&se=2025-05-01T02:01:08Z&spr=https&sv=2022-11-02&sr=c&sig=cVrFc2NcsOZ3U%2BITpMd1T%2BVMSEOBCAekpZhYQEIrd20%3D
ENV ACP_BLOB_CONTAINER=acpcontainer
ENV USE_REDIS=false


COPY target/AcpStorageService*.jar app.jar
ENTRYPOINT ["java", "-jar","/app.jar"]