package uk.ac.ed.controller;

import org.springframework.web.bind.annotation.*;
import uk.ac.ed.data.StorageDataDefinition;
import com.google.gson.Gson;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.UUID;

/**
 * the ILP Tutorial service which provides suppliers, orders and other useful things
 */
@RestController
public class StorageServiceController {

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
     * @return a unique id for the created data
     */
    @PostMapping(value = "/write",  consumes = {"*/*"})
    public String write(@RequestBody StorageDataDefinition data) throws IOException {
        String filename = UUID.randomUUID().toString();
        Files.writeString(Path.of(filename), new Gson().toJson(data), StandardCharsets.UTF_8);
        return filename;
    }

    @GetMapping(value = "/read/{uniqueId}")
    public StorageDataDefinition read(@PathVariable(required = true) String uniqueId) throws IOException {
        String data = Files.readString(Path.of(uniqueId));
        return new Gson().fromJson(data, StorageDataDefinition.class);
    }
}
