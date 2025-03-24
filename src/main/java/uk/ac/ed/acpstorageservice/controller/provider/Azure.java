package uk.ac.ed.acpstorageservice.controller.provider;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.params.SetParams;
import uk.ac.ed.acpstorageservice.data.RuntimeEnvironment;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

public class Azure extends AbstractStorageProvider {
    public Azure(RuntimeEnvironment runtimeEnvironment) {
        super(runtimeEnvironment);
    }

    @Override
    public UUID write(String data) throws IOException {
        UUID result = UUID.randomUUID();
        getBlobClient(result).upload(BinaryData.fromString(data));
        return result;
    }


    @Override
    public String read(UUID uniqueId) throws IOException {
        return getBlobClient(uniqueId).downloadContent().toString();
    }

    @Override
    public void delete(UUID uniqueId) throws IOException {
        getBlobClient(uniqueId).delete();
    }

    @Override
    public UUID[] list(Integer limit) {
        if (limit == null || limit == 0) {
            limit = Integer.MAX_VALUE;
        }

        return getBlobContainerClient(getBlobServiceClient()).listBlobs().stream().filter(f -> {
            try {
                UUID.fromString(f.getName());
                return true;
            } catch (Exception x) {
                return false;
            }
        }).map(e -> UUID.fromString(e.getName())).limit(limit).toArray(UUID[]::new);
    }

    private BlobClient getBlobClient(UUID uniqueId){
        var blobContainerClient = getBlobContainerClient(getBlobServiceClient());
        return blobContainerClient.getBlobClient(uniqueId.toString());
    }

    private BlobServiceClient getBlobServiceClient(){
        String azureConnectString = runtimeEnvironment.getAcpStorageConnection();
        if (azureConnectString == null || azureConnectString.isBlank()) {
            throw new RuntimeException(RuntimeEnvironment.ACP_STORAGE_CONNECTION + " is not set");
        }

        return new BlobServiceClientBuilder().connectionString(azureConnectString).buildClient();
    }


    private BlobContainerClient getBlobContainerClient(BlobServiceClient blobServiceClient) {
        String azureContainerName = runtimeEnvironment.getAcpBlobContainerName();
        if (azureContainerName == null || azureContainerName.isBlank()) {
            throw new RuntimeException(RuntimeEnvironment.ACP_BLOB_CONTAINER + " is not set");
        }

        return blobServiceClient.getBlobContainerClient(azureContainerName);
    }
}
