package uk.ac.ed.acpstorageservice.data;

import lombok.Getter;
import lombok.Setter;

/**
 * Represents configuration settings for runtime runtimeEnvironments, which are populated using runtimeEnvironment variables.
 * This class includes settings related to Redis, RabbitMQ, and Kafka. It provides static constants for
 * runtimeEnvironment variable names and methods to retrieve and validate the required runtimeEnvironment settings.
 */
@Getter
@Setter
public class RuntimeEnvironment {

    public static final String REDIS_HOST_ENV_VAR = "REDIS_HOST";
    public static final String REDIS_PORT_ENV_VAR = "REDIS_PORT";
    public static final String ACP_STORAGE_CONNECTION = "ACP_STORAGE_CONNECTION";

    // the container for Azure or the table for DynamoDb
    public static final String ACP_BLOB_CONTAINER = "ACP_BLOB_CONTAINER";

    public static final String ACP_STORAGE_DYNAMODB_REGION  = "ACP_STORAGE_DYNAMODB_REGION";

    public static final String USE_REDIS = "ACP_STORAGE_USE_REDIS";
    public static final String USE_AWS = "ACP_STORAGE_USE_AWS";

    private String redisHost;
    private int redisPort;
    private String acpStorageConnection;
    private String acpBlobContainerName;
    private boolean useRedis;
    private boolean useAws;
    private String dynamoDbRegion;

    /**
     * Configures and retrieves the runtime runtimeEnvironment settings by reading from
     * predefined runtimeEnvironment variables. If specific runtimeEnvironment variables are not
     * set, it uses default values for the configuration. Validates necessary variables
     * required for Kafka security setup if security is enabled.
     *
     * @return a configured {@code RuntimeruntimeEnvironment} object containing runtime settings
     *         such as Kafka, Redis, and RabbitMQ configurations.
     * @throws RuntimeException if Kafka security is enabled but the required security
     *         configuration variables are missing.
     */
    public static RuntimeEnvironment getruntimeEnvironment() {
        RuntimeEnvironment settings = new RuntimeEnvironment();

        settings.setUseAws(Boolean.parseBoolean(System.getenv(USE_AWS)));
        if (settings.isUseAws()) {
            if (System.getenv(ACP_STORAGE_DYNAMODB_REGION) == null) {
                throw new RuntimeException("no region for DynamoDB specified");
            }
            settings.setDynamoDbRegion(System.getenv(ACP_STORAGE_DYNAMODB_REGION));
        } else {
            if (System.getenv(ACP_STORAGE_CONNECTION) == null) {
                throw new RuntimeException("no ACP STORAGE CONNECTION specified");
            }
            settings.setAcpStorageConnection(System.getenv(ACP_STORAGE_CONNECTION));
        }

        if (System.getenv(ACP_BLOB_CONTAINER) == null) {
            throw new RuntimeException("no ACP BLOB CONTAINER specified");
        }
        settings.setAcpBlobContainerName(System.getenv(ACP_BLOB_CONTAINER));

        // if not set no redis will be used
        settings.setUseRedis(Boolean.parseBoolean(System.getenv(USE_REDIS)));
        settings.setRedisHost(System.getenv(REDIS_HOST_ENV_VAR) == null ? "localhost" : System.getenv(REDIS_HOST_ENV_VAR));
        settings.setRedisPort(System.getenv(REDIS_PORT_ENV_VAR) == null ? 6379 : Integer.parseInt(System.getenv(REDIS_PORT_ENV_VAR)));

        return settings;
    }
}
