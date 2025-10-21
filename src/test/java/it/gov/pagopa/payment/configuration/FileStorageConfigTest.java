package it.gov.pagopa.payment.configuration;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FileStorageConfigUnitTest {

  @Test
  void blobContainerClient_isBuiltFromProperties() {
    FileStorageProperties props = new FileStorageProperties();
    props.setStorageAccountName("testaccount");
    props.setContainerReference("test-container");

    FileStorageConfig config = new FileStorageConfig(props);

    BlobServiceClient serviceClient = config.blobServiceClient();
    BlobContainerClient containerClient = config.blobContainerClient(serviceClient);

    assertNotNull(containerClient);
    assertTrue(containerClient.getBlobContainerUrl()
        .contains("https://testaccount.blob.core.windows.net/test-container"));
  }
}
