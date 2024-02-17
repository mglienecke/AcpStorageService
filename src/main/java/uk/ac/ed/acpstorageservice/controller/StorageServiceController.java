package uk.ac.ed.acpstorageservice.controller;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.google.gson.Gson;
import org.springframework.web.bind.annotation.*;
import uk.ac.ed.acpstorageservice.data.StorageDataDefinition;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * the ILP Tutorial service which provides suppliers, orders and other useful things
 */
@RestController()
@RequestMapping("storage")
public class StorageServiceController {

    public static final String ACP_STORAGE_CONNECTION_STRING = "ACP_STORAGE_CONNECTION_STRING";
    public static final String ACP_CONTAINER_NAME = "ACP_CONTAINER_NAME";
    public static final String FILE = "file";
    public static final String BLOB = "blob";
    private String azureConnectString = null;
    private String azureContainerName = null;


    /**
     * a simple alive check
     *
     * @return true (always)
     */
    @GetMapping(value = {"/isAlive"})
    public boolean isAlive() {
        return true;
    }

    /**
     * POST with a JSON data structure in the request body
     * @param data is the definition of the data to write
     * @param target is the write destination (file or blob)
     * @return a unique id for the created data
     */
    @PostMapping(value = "/write/{target}",  consumes = {"*/*"})
    public String write(@PathVariable() String target, @RequestBody StorageDataDefinition data) throws IOException {
        String fileIdentifier = UUID.randomUUID().toString();
        target = target.toLowerCase();
        String dataToWrite = new Gson().toJson(data);

        switch (target){
            case FILE:
                Files.writeString(getFilePath(fileIdentifier), dataToWrite, StandardCharsets.UTF_8);
                break;
            case BLOB:
                var blobContainerClient = getBlobContainerClient(getBlobServiceClient());
                var blobClient = blobContainerClient.getBlobClient(fileIdentifier);
                blobClient.upload(BinaryData.fromString(dataToWrite));
                break;
            default:
                throw new RuntimeException("not supported");
        }

        return fileIdentifier;
    }


    @GetMapping(value = "/read/{source}/{uniqueId}")
    public StorageDataDefinition read(@PathVariable() String source, @PathVariable() String uniqueId) throws IOException {
        source = source.toLowerCase();
        String data;

        switch (source){
            case FILE:
                data = Files.readString(getFilePath(uniqueId));
                break;
            case BLOB:
                var blobContainerClient = getBlobContainerClient(getBlobServiceClient());
                var blobClient = blobContainerClient.getBlobClient(uniqueId);
                data = blobClient.downloadContent().toString();
                break;
            default:
                throw new RuntimeException("not supported");
        }

        return new Gson().fromJson(data, StorageDataDefinition.class);
    }

    private Path getFilePath(String uniqueId){
        return Path.of("/tmp", uniqueId);
    }

    private BlobServiceClient getBlobServiceClient(){
        return new BlobServiceClientBuilder().connectionString(azureConnectString).buildClient();
    }

    private BlobContainerClient getBlobContainerClient(BlobServiceClient blobServiceClient) {
        return blobServiceClient.getBlobContainerClient(azureContainerName);
    }

    /**
     * initialize the controller by reading the ENV variables
     */
    public StorageServiceController(){
        azureConnectString = System.getenv(ACP_STORAGE_CONNECTION_STRING);
        azureContainerName = System.getenv(ACP_CONTAINER_NAME);

        boolean error = false;

        if (azureConnectString == null || azureConnectString.isBlank()) {
            System.err.println(ACP_STORAGE_CONNECTION_STRING + " is not set");
            error = true;
        }

        if (azureContainerName == null || azureContainerName.isBlank()) {
            System.err.println(ACP_CONTAINER_NAME + " is not set");
            error = true;
        }

        if (error){
            System.err.println("terminating due to configuration errors...");
            System.exit(1);
        }
    }
}
