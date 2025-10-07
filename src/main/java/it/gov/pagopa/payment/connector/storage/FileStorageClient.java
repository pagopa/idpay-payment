package it.gov.pagopa.payment.connector.storage;

import com.azure.storage.blob.BlobContainerClient;
import it.gov.pagopa.common.storage.AzureBlobClientImpl;
import org.springframework.stereotype.Service;

@Service
public class FileStorageClient extends AzureBlobClientImpl {

  public FileStorageClient(BlobContainerClient fileStorageClientConfiguration) {
    super(fileStorageClientConfiguration);
  }
}
