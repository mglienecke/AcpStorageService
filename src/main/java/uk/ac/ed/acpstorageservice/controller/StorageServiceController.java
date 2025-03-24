package uk.ac.ed.acpstorageservice.controller;

import com.azure.core.annotation.QueryParam;
import com.google.gson.Gson;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;
import redis.clients.jedis.params.SetParams;
import uk.ac.ed.acpstorageservice.configuration.AppConfig;
import uk.ac.ed.acpstorageservice.data.RuntimeEnvironment;
import uk.ac.ed.acpstorageservice.data.StorageDataDefinition;

import java.io.IOException;
import java.util.UUID;

import redis.clients.jedis.JedisPool;

/**
 * Controller for managing storage services through REST APIs.
 * This class provides endpoints to interact with different storage targets
 * such as files and blobs for writing, reading, deleting, and listing data.
 */
@RestController()
@RequestMapping("/api/v1")
public class StorageServiceController {


    public static final String TMP = "/tmp";

    private final RuntimeEnvironment runtimeEnvironment;

    public StorageServiceController(RuntimeEnvironment runtimeEnvironment) {
        this.runtimeEnvironment = runtimeEnvironment;
    }

    /**
     * Handles HTTP GET requests to retrieve an array of target types.
     *
     * @return an array of target types as strings, such as "FILE" and "BLOB"
     */
    @GetMapping(value = "/targets")
    public String[] targets() {
        return new String[]{"FILE", "BLOB"};
    }


    /**
     * Handles HTTP POST requests to write data to the specified target.
     *
     * @param target the target identifier where the data will be written
     * @param data the JSON string containing the data to be written
     * @return a UUID representing the identifier of the written data
     * @throws IOException if an input or output exception occurs during the operation
     */
    @PostMapping(value = "/{target}",  consumes = {"application/json"})
    public UUID write(@PathVariable() String target, @RequestBody String data) throws IOException {
        return internalWrite(data);
    }


    /**
     * Handles the HTTP POST request to store structured data at a specific target location.
     *
     * @param target the target location where the structured data will be stored
     * @param data the structured data to be stored, represented as a StorageDataDefinition object
     * @return a UUID that uniquely identifies the stored data
     * @throws IOException if an I/O error occurs during the data storage process
     */
    @PostMapping(value = "/data_definition/{target}",  consumes = {"application/json"})
    public UUID writeStorageDataDefinition(@PathVariable() String target, @RequestBody StorageDataDefinition data) throws IOException {
        return internalWrite(new Gson().toJson(data));
    }


    /**
     * Writes data to a specified target (e.g., file or blob storage) and optionally stores it in Redis if enabled.
     *
     * @param data the content to be written to the specified target
     * @return a UUID representing the identifier for the written data
     * @throws IOException if an I/O error occurs while writing to the file or blob storage
     */
    private UUID internalWrite(String data) throws IOException {
        UUID result = new AppConfig().StorageProvider(runtimeEnvironment).write(data);

        if (runtimeEnvironment.isUseRedis()) {
            // now store in Redis
            try (JedisPool pool = new JedisPool(runtimeEnvironment.getRedisHost(), runtimeEnvironment.getRedisPort()); var jedis = pool.getResource()) {
                var params = new SetParams();
                params.ex(2);
                jedis.set(result.toString(), data, params);
            }
        }

        return result;
    }


    /**
     * Reads and retrieves a string based on the specified source and unique identifier.
     *
     * @param source the data source to be used for the read operation
     * @param uniqueId the unique identifier for the specific resource in the source
     * @return a string corresponding to the resource identified by the provided source and uniqueId
     * @throws IOException if an input or output exception occurs during the operation
     */
    @GetMapping(value = "/{source}/{uniqueId}")
    public String read(@PathVariable() String source, @PathVariable() UUID uniqueId) throws IOException {
        return internalRead(uniqueId);
    }


    /**
     * Reads and retrieves a storage data definition based on the provided source and unique ID.
     *
     * @param source the source identifier from which the data definition will be fetched
     * @param uniqueId the unique identifier for the specific data definition
     * @return the storage data definition mapped to the StorageDataDefinition class
     * @throws IOException if an input or output operation fails during the retrieval process
     */
    @GetMapping(value = "/data_definition/{source}/{uniqueId}")
    public StorageDataDefinition readStorageDataDefinition(@PathVariable() String source, @PathVariable() UUID uniqueId) throws IOException {
        return new Gson().fromJson(internalRead(uniqueId), StorageDataDefinition.class);
    }


    /**
     * Reads data based on the given source type and unique identifier. This method determines the storage mechanism
     * to use (e.g., Redis, file system, or blob storage) and retrieves the corresponding data. If Redis is used,
     * it checks for data in a Redis store. Otherwise, it falls back to reading from a file or blob storage.
     *
     * @param uniqueId the unique identifier of the data to be read
     * @return the retrieved data as a string
     * @throws IOException if an I/O error occurs during file operations
     */
    private String internalRead(UUID uniqueId) throws IOException {
        String data = null;

        if (runtimeEnvironment.isUseRedis()) {
            // now store in Redis
            try (JedisPool pool = new JedisPool(runtimeEnvironment.getRedisHost(), runtimeEnvironment.getRedisPort()); var jedis = pool.getResource()) {
                data = jedis.get(uniqueId.toString());
            }
        }

        if (data == null) {
            data = new AppConfig().StorageProvider(runtimeEnvironment).read(uniqueId);
        }

        return data;
    }

    /**
     * Deletes a resource specified by its unique identifier and source type.
     * The source can be either a file or a blob. It also handles Redis cleanup if enabled in the runtimeEnvironment.
     *
     * @param source The type of the resource to delete (e.g., FILE or BLOB).
     * @param uniqueId The unique identifier of the resource to be deleted.
     * @throws IOException If an error occurs while attempting to delete the resource.
     */
    @DeleteMapping(value = "/{source}/{uniqueId}")
    public void delete(@PathVariable() String source, @PathVariable() @NonNull UUID uniqueId) throws IOException {

        try {
            new AppConfig().StorageProvider(runtimeEnvironment).delete(uniqueId);

            if (runtimeEnvironment.isUseRedis()) {
                try (JedisPool pool = new JedisPool(runtimeEnvironment.getRedisHost(), runtimeEnvironment.getRedisPort()); var jedis = pool.getResource()) {
                    jedis.unlink(uniqueId.toString());
                }
            }
        } catch (IOException | RuntimeException e) {
            System.err.println("Ooops: " + e.getMessage());
        }
    }


    /**
     * Retrieves a list of UUIDs from a specified source. The source can either be a directory of files
     * or a blob storage container. The method filters and collects only those entries that can be
     * successfully parsed into UUIDs.
     *
     * @param source the source from which to retrieve UUIDs, accepted values are "FILE" and "BLOB"
     * @return an array of UUIDs parsed from the specified source
     * @throws IOException if an I/O error occurs while accessing the file system or blob storage
     * @throws RuntimeException if the specified source is unsupported
     */
    @GetMapping(value = "/{source}", produces = "application/json")
    public UUID[] list(@PathVariable() String source, @QueryParam("limit") Integer limit) throws IOException {
        return new AppConfig().StorageProvider(runtimeEnvironment).list(limit);
    }
}
