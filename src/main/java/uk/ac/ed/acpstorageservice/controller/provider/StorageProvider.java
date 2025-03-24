package uk.ac.ed.acpstorageservice.controller.provider;

import uk.ac.ed.acpstorageservice.data.RuntimeEnvironment;

import java.io.IOException;
import java.util.UUID;

public interface StorageProvider {
    UUID write(String data) throws IOException;
    String read(UUID uniqueId) throws IOException;
    void delete(UUID uniqueId) throws IOException;
    UUID[] list(Integer limit);
}
