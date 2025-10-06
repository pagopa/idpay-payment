package it.gov.pagopa.payment.configuration;

import com.azure.storage.blob.BlobContainerClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = FileStorageConfig.class)
@TestPropertySource(properties = {
        "blobStorage.connectionString=DefaultEndpointsProtocol=https;AccountName=test;AccountKey=test;EndpointSuffix=core.windows.net",
        "blobStorage.containerReference=test-container"
})
class FileStorageConfigTest {

    @Autowired
    private ApplicationContext context;

    @Test
    void fileStorageClientConfiguration_createsBean() {
        BlobContainerClient client = context.getBean(BlobContainerClient.class);
        assertNotNull(client);
        assertEquals("test-container", client.getBlobContainerName());
    }
}