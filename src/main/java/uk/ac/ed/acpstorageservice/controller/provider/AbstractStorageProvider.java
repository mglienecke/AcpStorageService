package uk.ac.ed.acpstorageservice.controller.provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ed.acpstorageservice.data.RuntimeEnvironment;

public abstract class AbstractStorageProvider implements StorageProvider {
    protected RuntimeEnvironment runtimeEnvironment;
    protected Logger logger = LoggerFactory.getLogger(AbstractStorageProvider.class);

    protected AbstractStorageProvider(RuntimeEnvironment runtimeruntimeEnvironment) {
        this.runtimeEnvironment = runtimeruntimeEnvironment;
    }
}
