package uk.ac.ed.acpstorageservice.controller;

import com.google.gson.Gson;
import org.springframework.web.bind.annotation.*;
import uk.ac.ed.acpstorageservice.data.StorageDataDefinition;

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
        String fileIdentifier;
        target = target.toLowerCase();

        switch (target){
            case "file":
                fileIdentifier = writeFile(data);
                break;
            case "blob":
                throw new RuntimeException("BLOB not yet supported");
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
            case "file":
                data = Files.readString(getFilePath(uniqueId));
                break;
            case "blob":
                throw new RuntimeException("BLOB not yet supported");
            default:
                throw new RuntimeException("not supported");
        }

        return new Gson().fromJson(data, StorageDataDefinition.class);
    }

    private Path getFilePath(String uniqueId){
        return Path.of("/tmp", uniqueId);
    }

    private String writeFile(StorageDataDefinition data) throws IOException {
        String filename = UUID.randomUUID().toString();
        Files.writeString(getFilePath(filename), new Gson().toJson(data), StandardCharsets.UTF_8);
        return filename;
    }
}
