package uk.ac.ed.acpstorageservice.data;
import jakarta.annotation.Generated;
import lombok.Getter;
import lombok.Setter;

/**
 * defines the storage data to write
 */
@Getter
@Setter
public class StorageDataDefinition {

    private String uid;
    private String datasetName;
    private String data;

    /**
     * Constructs a new StorageDataDefinition instance with the specified parameters.
     *
     * @param uid the unique identifier associated with the data
     * @param datasetName the name of the dataset to which the data belongs
     * @param data the actual data content to be stored
     */
    public StorageDataDefinition(String uid, String datasetName, String data) {
        this.uid = uid;
        this.data = data;
        this.datasetName = datasetName;
    }

    public StorageDataDefinition(){

    }
}
