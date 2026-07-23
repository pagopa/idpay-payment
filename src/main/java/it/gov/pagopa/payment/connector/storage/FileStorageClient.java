package it.gov.pagopa.payment.connector.storage;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import it.gov.pagopa.common.storage.AzureBlobClientImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class FileStorageClient extends AzureBlobClientImpl {

  public FileStorageClient(BlobContainerClient fileStorageClientConfiguration,
                           BlobServiceClient blobServiceClient,
                           @Value("${storage.file.max-retry:3600}")
                           Integer sasDurationSeconds) {
    super(fileStorageClientConfiguration, blobServiceClient, sasDurationSeconds);
  }
}
