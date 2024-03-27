package uk.ac.ed.acpstorageservice.controller;

import com.azure.core.annotation.QueryParam;
import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.models.ListBlobsOptions;
import com.google.gson.Gson;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;
import redis.clients.jedis.params.SetParams;
import uk.ac.ed.acpstorageservice.data.StorageDataDefinition;

import java.io.BufferedInputStream;
import java.io.Console;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
/**
 * the ILP Tutorial service which provides suppliers, orders and other useful things
 */
@RestController()
// @RequestMapping("storage")
public class StorageServiceController {

    public static final String ACP_STORAGE_CONNECTION_STRING = "ACP_STORAGE_CONNECTION_STRING";
    public static final String ACP_CONTAINER_NAME = "ACP_CONTAINER_NAME";
    public static final String FILE = "file";
    public static final String BLOB = "blob";
    public static final String TMP = "/tmp";


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
    public UUID write(@PathVariable() String target, @RequestBody StorageDataDefinition data) throws IOException {
        UUID result = UUID.randomUUID();
        target = target.toLowerCase();
        String dataToWrite = new Gson().toJson(data);

        switch (target){
            case FILE:
                Files.writeString(getFilePath(result.toString()), dataToWrite, StandardCharsets.UTF_8);
                break;
            case BLOB:
                getBlobClient(result).upload(BinaryData.fromString(dataToWrite));
                break;
            default:
                throw new RuntimeException("not supported");
        }

        // now store in Redis
        JedisPool pool = new JedisPool("localhost", 6379);

        try (var jedis = pool.getResource()) {
            var params = new SetParams();
            params.ex(2);
            jedis.set(result.toString(), dataToWrite, params);
        }

        return result;
    }


    /**
     * read a data item using a UUID
     * @param source is where the data is stored (file or blob)
     * @param uniqueId is the UUID for the data item
     * @return a storage data definition
     * @throws IOException
     */
    @GetMapping(value = "/read/{source}/{uniqueId}")
    public StorageDataDefinition read(@PathVariable() String source, @PathVariable() UUID uniqueId) throws IOException {
        source = source.toLowerCase();
        String data;

        // now store in Redis
        JedisPool pool = new JedisPool("localhost", 6379);

        try (var jedis = pool.getResource()) {
            data = jedis.get(uniqueId.toString());
            if (data  == null) {
                switch (source){
                    case FILE:
                        data = Files.readString(getFilePath(uniqueId.toString()));
                        break;
                    case BLOB:
                        data = getBlobClient(uniqueId).downloadContent().toString();
                        break;
                    default:
                        throw new RuntimeException("not supported");
                }
            }
        }

        return new Gson().fromJson(data, StorageDataDefinition.class);
    }
    @DeleteMapping(value = "/{source}/{uniqueId}")
    public void delete(@PathVariable() String source, @PathVariable() @NonNull UUID uniqueId) throws IOException {
        source = source.toLowerCase();

        try {
            switch (source){
                case FILE:
                    Files.delete(getFilePath(uniqueId.toString()));
                    break;
                case BLOB:
                    getBlobClient(uniqueId).delete();
                    break;
                default:
                    throw new RuntimeException("not supported");
            }

            // now store in Redis
            JedisPool pool = new JedisPool("localhost", 6379);

            try (var jedis = pool.getResource()) {
                jedis.unlink(uniqueId.toString());
            }

        } catch (IOException | RuntimeException e) {
            System.err.println("Ooops: " + e.getMessage());
        }
    }


    /**
     * perform a list operation by streaming all elements of either file or blob and those which have a UUID as name will be returned
     * @param source defines where to search
     * @return a list of UUID names (can be empty)
     * @throws IOException
     */
    @GetMapping(value = "/list/{source}")
    public UUID[] list(@PathVariable() String source) throws IOException {
        source = source.toLowerCase();
        UUID[] result = new UUID[0];

        switch (source){
            case FILE:
                result = Files.list(Path.of(TMP)).filter(f -> {
                    try {
                        UUID.fromString(f.getFileName().toString());
                        return true;
                    } catch (Exception x) {
                        return false;
                    }
                }).map(e -> UUID.fromString(e.getFileName().toString())).toArray(UUID[]::new);
                break;
            case BLOB:
                var blobContainerClient = getBlobContainerClient(getBlobServiceClient());
                result = blobContainerClient.listBlobs().stream().filter(f -> {
                    try {
                        UUID.fromString(f.getName());
                        return true;
                    } catch (Exception x) {
                        return false;
                    }
                }).map(e -> UUID.fromString(e.getName())).toArray(UUID[]::new);
                break;
            default:
                throw new RuntimeException("not supported");
        }

        return result;
    }

    private Path getFilePath(String uniqueId){
        return Path.of(TMP, uniqueId);
    }

    private BlobClient getBlobClient(UUID uniqueId){
        var blobContainerClient = getBlobContainerClient(getBlobServiceClient());
        return blobContainerClient.getBlobClient(uniqueId.toString());
    }

    private BlobServiceClient getBlobServiceClient(){
        String azureConnectString = System.getenv(ACP_STORAGE_CONNECTION_STRING);
        if (azureConnectString == null || azureConnectString.isBlank()) {
            throw new RuntimeException(ACP_STORAGE_CONNECTION_STRING + " is not set");
        }

        return new BlobServiceClientBuilder().connectionString(azureConnectString).buildClient();
    }

    private BlobContainerClient getBlobContainerClient(BlobServiceClient blobServiceClient) {
        String azureContainerName = System.getenv(ACP_CONTAINER_NAME);
        if (azureContainerName == null || azureContainerName.isBlank()) {
            throw new RuntimeException(ACP_CONTAINER_NAME + " is not set");
        }

        return blobServiceClient.getBlobContainerClient(azureContainerName);
    }
}
