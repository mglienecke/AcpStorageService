package uk.ac.ed.acpstorageservice.data;

/**
 * defines the storage data to write
 */
public class StorageDataDefinition {

    private String uid;
    private String datasetName;
    private String data;


    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getDatasetName() {
        return datasetName;
    }

    public void setDatasetName(String datasetName) {
        this.datasetName = datasetName;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }


    /**
     * create a new instance of a storage data definition
     * @param uid
     * @param datasetName
     * @param data
     */
    public StorageDataDefinition(String uid, String datasetName, String data) {
        this.uid = uid;
        this.data = data;
        this.datasetName = datasetName;
    }

    public StorageDataDefinition(){

    }
}
