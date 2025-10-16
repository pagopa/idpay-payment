package it.gov.pagopa.common.storage;

import com.azure.core.http.rest.PagedIterable;
import com.azure.core.http.rest.Response;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.*;
import com.azure.storage.blob.options.*;
import it.gov.pagopa.payment.connector.storage.FileStorageClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileStorageClientTest {

  @Mock
  private BlobContainerClient blobContainerClient;

  @Mock
  private BlobClient blobClient;

  private FileStorageClient fileStorageClient;

  @BeforeEach
  void setUp() {
    fileStorageClient = new FileStorageClient(blobContainerClient);
  }

  @Mock
  private Response<BlockBlobItem> mockResponse;

  @Test
  void uploadFile_ShouldUploadSuccessfully() {
    File file = new File("test.txt");
    String destination = "folder/test.txt";
    String contentType = "text/plain";

    when(blobContainerClient.getBlobClient(destination)).thenReturn(blobClient);
    when(blobClient.uploadFromFileWithResponse(any(BlobUploadFromFileOptions.class), any(), any()))
        .thenReturn(mockResponse);

    Response<BlockBlobItem> result = fileStorageClient.uploadFile(file, destination, contentType);

    assertThat(result).isEqualTo(mockResponse);
    verify(blobContainerClient).getBlobClient(destination);
    verify(blobClient).uploadFromFileWithResponse(any(BlobUploadFromFileOptions.class), any(), any());
  }

  @Test
  void upload_ShouldUploadFromInputStream() {
    InputStream inputStream = new ByteArrayInputStream("test content".getBytes());
    String destination = "folder/test.txt";
    String contentType = "text/plain";

    Response<BlockBlobItem> mockResponse = mock(Response.class);

    when(blobContainerClient.getBlobClient(destination)).thenReturn(blobClient);
    when(blobClient.uploadWithResponse(any(BlobParallelUploadOptions.class), any(), any()))
        .thenReturn(mockResponse);

    Response<BlockBlobItem> result = fileStorageClient.upload(inputStream, destination, contentType);

    assertThat(result).isEqualTo(mockResponse);
    verify(blobContainerClient).getBlobClient(destination);
    verify(blobClient).uploadWithResponse(any(BlobParallelUploadOptions.class), any(), any());
  }

  @Test
  void deleteFile_ShouldDeleteSuccessfully() {
    String destination = "folder/test.txt";
    Response<Boolean> mockResponse = mock(Response.class);

    when(blobContainerClient.getBlobClient(destination)).thenReturn(blobClient);
    when(blobClient.deleteIfExistsWithResponse(any(), any(), any(), any()))
        .thenReturn(mockResponse);

    Response<Boolean> result = fileStorageClient.deleteFile(destination);

    assertThat(result).isEqualTo(mockResponse);
    verify(blobContainerClient).getBlobClient(destination);
    verify(blobClient).deleteIfExistsWithResponse(any(), any(), any(), any());
  }

  @Test
  void download_ShouldReturnNullWhen404() {
    String filePath = "nonexistent.txt";
    BlobStorageException exception = mock(BlobStorageException.class);
    when(exception.getStatusCode()).thenReturn(404);

    when(blobContainerClient.getBlobClient(filePath)).thenReturn(blobClient);
    doThrow(exception).when(blobClient).downloadStream(any(OutputStream.class));

    ByteArrayOutputStream result = fileStorageClient.download(filePath);

    assertThat(result).isNull();
    verify(blobContainerClient).getBlobClient(filePath);
    verify(blobClient).downloadStream(any(OutputStream.class));
  }

  @Test
  void download_ShouldThrowExceptionWhenServerError() {
    String filePath = "test.txt";
    BlobStorageException exception = mock(BlobStorageException.class);
    when(exception.getStatusCode()).thenReturn(500);

    when(blobContainerClient.getBlobClient(filePath)).thenReturn(blobClient);
    doThrow(exception).when(blobClient).downloadStream(any(OutputStream.class));

    assertThatThrownBy(() -> fileStorageClient.download(filePath))
        .isInstanceOf(BlobStorageException.class);
  }

  @Test
  void downloadToFile_ShouldDownloadSuccessfully() throws IOException {
    String filePath = "test.txt";
    Path destination = Files.createTempFile("test", ".txt");

    Response<BlobProperties> mockResponse = mock(Response.class);

    when(blobContainerClient.getBlobClient(filePath)).thenReturn(blobClient);
    when(blobClient.downloadToFileWithResponse(any(BlobDownloadToFileOptions.class), any(), any()))
        .thenReturn(mockResponse);

    try {
      Response<BlobProperties> result = fileStorageClient.download(filePath, destination);

      assertThat(result).isEqualTo(mockResponse);
      verify(blobContainerClient).getBlobClient(filePath);
      verify(blobClient).downloadToFileWithResponse(any(BlobDownloadToFileOptions.class), any(), any());
    } finally {
      Files.deleteIfExists(destination);
    }
  }

  @Test
  void listFiles_ShouldReturnPagedIterable() {
    String path = "folder/";
    PagedIterable<BlobItem> mockPagedIterable = mock(PagedIterable.class);

    when(blobContainerClient.listBlobsByHierarchy(path)).thenReturn(mockPagedIterable);

    PagedIterable<BlobItem> result = fileStorageClient.listFiles(path);

    assertThat(result).isEqualTo(mockPagedIterable);
    verify(blobContainerClient).listBlobsByHierarchy(path);
  }
}
