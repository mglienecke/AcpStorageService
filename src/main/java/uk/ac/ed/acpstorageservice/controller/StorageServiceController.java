package uk.ac.ed.acpstorageservice.controller;

import com.azure.core.annotation.Get;
import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.google.gson.Gson;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;
import redis.clients.jedis.params.SetParams;
import uk.ac.ed.acpstorageservice.data.RuntimeEnvironment;
import uk.ac.ed.acpstorageservice.data.StorageDataDefinition;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import redis.clients.jedis.JedisPool;
/**
 * the ILP Tutorial service which provides suppliers, orders and other useful things
 */
@RestController()
@RequestMapping("/api/v1")
public class StorageServiceController {
    public static final String FILE = "file";
    public static final String BLOB = "blob";
    public static final String TMP = "/tmp";
    private final RuntimeEnvironment environment;
    private final RuntimeEnvironment runtimeEnvironment;

    public StorageServiceController(RuntimeEnvironment environment, RuntimeEnvironment runtimeEnvironment) {
        this.environment = environment;
        this.runtimeEnvironment = runtimeEnvironment;
    }

    /**
     * POST with a JSON data structure in the request body
     * @param data is the definition of the data to write
     * @param target is the write destination (file or blob)
     * @return a unique id for the created data
     */
    @PostMapping(value = "/{target}",  consumes = {"application/json"})
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

        if (environment.isUseRedis()) {
            // now store in Redis
            try (JedisPool pool = new JedisPool(environment.getRedisHost(), environment.getRedisPort()); var jedis = pool.getResource()) {
                var params = new SetParams();
                params.ex(2);
                jedis.set(result.toString(), dataToWrite, params);
            }
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
    @GetMapping(value = "/{source}/{uniqueId}")
    public StorageDataDefinition read(@PathVariable() String source, @PathVariable() UUID uniqueId) throws IOException {
        source = source.toLowerCase();
        String data = null;

        if (environment.isUseRedis()) {
            // now store in Redis
            try (JedisPool pool = new JedisPool(environment.getRedisHost(), environment.getRedisPort()); var jedis = pool.getResource()) {
                data = jedis.get(uniqueId.toString());
            }
        }

        if (data == null) {
            data = switch (source) {
                case FILE -> Files.readString(getFilePath(uniqueId.toString()));
                case BLOB -> getBlobClient(uniqueId).downloadContent().toString();
                default -> throw new RuntimeException("not supported");
            };
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

            if (environment.isUseRedis()) {
                try (JedisPool pool = new JedisPool(environment.getRedisHost(), environment.getRedisPort()); var jedis = pool.getResource()) {
                    jedis.unlink(uniqueId.toString());
                }
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
    @GetMapping(value = "/{source}")
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

    @GetMapping(value = "/configData")
    public Map<String, String> getConfigData() {
        return Map.of(
                RuntimeEnvironment.ACP_STORAGE_CONNECTION, environment.getAcpStorageConnection(),
                RuntimeEnvironment.ACP_BLOB_CONTAINER, environment.getAcpBlobContainerName(),
                RuntimeEnvironment.USE_REDIS, environment.isUseRedis() ? "true" : "false",
                RuntimeEnvironment.REDIS_HOST_ENV_VAR, environment.getRedisHost(),
                RuntimeEnvironment.REDIS_PORT_ENV_VAR, String.valueOf(environment.getRedisPort()));
    }

    private Path getFilePath(String uniqueId){
        return Path.of(TMP, uniqueId);
    }

    private BlobClient getBlobClient(UUID uniqueId){
        var blobContainerClient = getBlobContainerClient(getBlobServiceClient());
        return blobContainerClient.getBlobClient(uniqueId.toString());
    }

    private BlobServiceClient getBlobServiceClient(){
        String azureConnectString = environment.getAcpStorageConnection();
        if (azureConnectString == null || azureConnectString.isBlank()) {
            throw new RuntimeException(RuntimeEnvironment.ACP_STORAGE_CONNECTION + " is not set");
        }

        System.out.println("connect string: " + azureConnectString);

        return new BlobServiceClientBuilder().connectionString(azureConnectString).buildClient();
    }

    private BlobContainerClient getBlobContainerClient(BlobServiceClient blobServiceClient) {
        String azureContainerName = environment.getAcpBlobContainerName();
        if (azureContainerName == null || azureContainerName.isBlank()) {
            throw new RuntimeException(RuntimeEnvironment.ACP_BLOB_CONTAINER + " is not set");
        }

        return blobServiceClient.getBlobContainerClient(azureContainerName);
    }
}
