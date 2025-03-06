package uk.ac.ed.acpstorageservice.data;

import lombok.Getter;
import lombok.Setter;

/**
 * Represents configuration settings for runtime environments, which are populated using environment variables.
 * This class includes settings related to Redis, RabbitMQ, and Kafka. It provides static constants for
 * environment variable names and methods to retrieve and validate the required environment settings.
 */
@Getter
@Setter
public class RuntimeEnvironment {

    public static final String REDIS_HOST_ENV_VAR = "REDIS_HOST";
    public static final String REDIS_PORT_ENV_VAR = "REDIS_PORT";
    public static final String ACP_STORAGE_CONNECTION = "ACP_STORAGE_CONNECTION";
    public static final String ACP_BLOB_CONTAINER = "ACP_BLOB_CONTAINER";
    public static final String USE_REDIS = "USE_REDIS";

    private String redisHost;
    private int redisPort;
    private String acpStorageConnection;
    private String acpBlobContainerName;
    private boolean useRedis;

    /**
     * Configures and retrieves the runtime environment settings by reading from
     * predefined environment variables. If specific environment variables are not
     * set, it uses default values for the configuration. Validates necessary variables
     * required for Kafka security setup if security is enabled.
     *
     * @return a configured {@code RuntimeEnvironment} object containing runtime settings
     *         such as Kafka, Redis, and RabbitMQ configurations.
     * @throws RuntimeException if Kafka security is enabled but the required security
     *         configuration variables are missing.
     */
    public static RuntimeEnvironment getEnvironment() {
        RuntimeEnvironment settings = new RuntimeEnvironment();

        if (System.getenv(ACP_BLOB_CONTAINER) == null) {
            throw new RuntimeException("no ACP BLOB CONTAINER specified");
        }

        if (System.getenv(ACP_STORAGE_CONNECTION) == null) {
            throw new RuntimeException("no ACP STORAGE CONNECTION specified");
        }

        // if not set no redis will be used
        settings.setUseRedis(System.getenv(USE_REDIS) != null && Boolean.parseBoolean(System.getenv(USE_REDIS)));
        settings.setRedisHost(System.getenv(REDIS_HOST_ENV_VAR) == null ? "localhost" : System.getenv(REDIS_HOST_ENV_VAR));
        settings.setRedisPort(System.getenv(REDIS_PORT_ENV_VAR) == null ? 6379 : Integer.parseInt(System.getenv(REDIS_PORT_ENV_VAR)));
        settings.setAcpBlobContainerName(System.getenv(ACP_BLOB_CONTAINER));
        settings.setAcpStorageConnection(System.getenv(ACP_STORAGE_CONNECTION));

        return settings;
    }
}
