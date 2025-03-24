package uk.ac.ed.acpstorageservice.controller.provider;

import org.slf4j.LoggerFactory;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;
import uk.ac.ed.acpstorageservice.data.RuntimeEnvironment;

import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Amazon extends AbstractStorageProvider {
    Logger logger = LoggerFactory.getLogger(Amazon.class);

    public Amazon(RuntimeEnvironment runtimeruntimeEnvironment) {
        super(runtimeruntimeEnvironment);
    }

    @Override
    public UUID write(String data) throws IOException {
        try (DynamoDbClient ddb = DynamoDbClient.builder()
                .region(Region.of(runtimeEnvironment.getDynamoDbRegion()))
                .build()) {

            var uuid = UUID.randomUUID();
            HashMap<String, AttributeValue> itemValues = new HashMap<>();
            itemValues.put("id", AttributeValue.builder().s(uuid.toString()).build());
            itemValues.put("data", AttributeValue.builder().s(data).build());
            PutItemRequest request = PutItemRequest.builder()
                    .tableName(runtimeEnvironment.getAcpBlobContainerName())
                    .item(itemValues)
                    .build();

            ddb.putItem(request);
            return uuid;
        } catch (Exception e) {
            logger.error("Error listing blobs", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public String read(UUID uniqueId) throws IOException {
        return "";
    }

    @Override
    public void delete(UUID uniqueId) throws IOException {

    }

    @Override
    public UUID[] list(Integer limit) {
        try (DynamoDbClient ddb = DynamoDbClient.builder()
                .region(Region.of(runtimeEnvironment.getDynamoDbRegion()))
                .build()) {

            ScanRequest scanRequest = ScanRequest.builder()
                    .tableName(runtimeEnvironment.getAcpBlobContainerName())
                    .limit(limit)
                    .build();

            ScanResponse response = ddb.scan(scanRequest);
            return response.items().stream().map(e -> UUID.fromString(e.get("id").s())).toArray(UUID[]::new);
        } catch (Exception e) {
            logger.error("Error listing blobs", e);
            throw new RuntimeException(e);
        }
    }
}
