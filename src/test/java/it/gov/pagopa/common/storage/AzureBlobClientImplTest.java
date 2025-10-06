package it.gov.pagopa.common.storage;

import com.azure.core.http.rest.PagedIterable;
import com.azure.core.http.rest.Response;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.models.BlobProperties;
import com.azure.storage.blob.models.BlockBlobItem;
import com.azure.storage.blob.models.BlobStorageException;
import com.azure.storage.blob.options.BlobDownloadToFileOptions;
import com.azure.storage.blob.options.BlobParallelUploadOptions;
import com.azure.storage.blob.options.BlobUploadFromFileOptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;

class AzureBlobClientImplTest {
    @Mock
    private BlobContainerClient blobContainerClient;
    @Mock
    private BlobClient blobClient;
    @Mock
    private Response<BlockBlobItem> blockBlobItemResponse;
    @Mock
    private Response<Boolean> booleanResponse;
    @Mock
    private Response<BlobProperties> blobPropertiesResponse;
    @Mock
    private PagedIterable<BlobItem> pagedIterable;

    private AzureBlobClientImpl azureBlobClient;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        Mockito.when(blobContainerClient.getBlobClient(anyString())).thenReturn(blobClient);
        azureBlobClient = new AzureBlobClientImpl(blobContainerClient);
    }

    @Test
    void uploadFile_shouldCallUploadFromFileWithResponse() {
        File file = new File("test.txt");
        Mockito.when(blobClient.uploadFromFileWithResponse(any(BlobUploadFromFileOptions.class), any(), any())).thenReturn(blockBlobItemResponse);
        Response<BlockBlobItem> response = azureBlobClient.uploadFile(file, "dest", "text/plain");
        assertEquals(blockBlobItemResponse, response);
        Mockito.verify(blobClient).uploadFromFileWithResponse(any(BlobUploadFromFileOptions.class), any(), any());
    }

    @Test
    void upload_shouldCallUploadWithResponse() {
        InputStream is = new ByteArrayInputStream("test".getBytes());
        Mockito.when(blobClient.uploadWithResponse(any(BlobParallelUploadOptions.class), any(), any())).thenReturn(blockBlobItemResponse);
        Response<BlockBlobItem> response = azureBlobClient.upload(is, "dest", "text/plain");
        assertEquals(blockBlobItemResponse, response);
        Mockito.verify(blobClient).uploadWithResponse(any(BlobParallelUploadOptions.class), any(), any());
    }

    @Test
    void deleteFile_shouldCallDeleteIfExistsWithResponse() {
        Mockito.when(blobClient.deleteIfExistsWithResponse(any(), any(), any(), any())).thenReturn(booleanResponse);
        Response<Boolean> response = azureBlobClient.deleteFile("dest");
        assertEquals(booleanResponse, response);
        Mockito.verify(blobClient).deleteIfExistsWithResponse(any(), any(), any(), any());
    }

    @Test
    void listFiles_shouldCallListBlobsByHierarchy() {
        Mockito.when(blobContainerClient.listBlobsByHierarchy(anyString())).thenReturn(pagedIterable);
        PagedIterable<BlobItem> result = azureBlobClient.listFiles("path");
        assertEquals(pagedIterable, result);
        Mockito.verify(blobContainerClient).listBlobsByHierarchy("path");
    }

    @Test
    void download_withPath_shouldCallDownloadToFileWithResponse() {
        Path tempFile = Paths.get(System.getProperty("java.io.tmpdir"), "test.txt");
        Mockito.when(blobClient.downloadToFileWithResponse(any(BlobDownloadToFileOptions.class), any(), any())).thenReturn(blobPropertiesResponse);
        Response<BlobProperties> response = azureBlobClient.download("file.txt", tempFile);
        assertEquals(blobPropertiesResponse, response);
        Mockito.verify(blobClient).downloadToFileWithResponse(any(BlobDownloadToFileOptions.class), any(), any());
    }

    @Test
    void download_withPath_blobStorageExceptionNot404_shouldThrow() {
        Path tempFile = Paths.get(System.getProperty("java.io.tmpdir"), "test.txt");
        BlobStorageException ex = Mockito.mock(BlobStorageException.class);
        Mockito.when(ex.getStatusCode()).thenReturn(500);
        Mockito.when(blobClient.downloadToFileWithResponse(any(BlobDownloadToFileOptions.class), any(), any())).thenThrow(ex);
        assertThrows(BlobStorageException.class, () -> azureBlobClient.download("file.txt", tempFile));
    }

    @Test
    void download_withPath_blobStorageException404_shouldReturnNull() {
        Path tempFile = Paths.get(System.getProperty("java.io.tmpdir"), "test.txt");
        BlobStorageException ex = Mockito.mock(BlobStorageException.class);
        Mockito.when(ex.getStatusCode()).thenReturn(404);
        Mockito.when(blobClient.downloadToFileWithResponse(any(BlobDownloadToFileOptions.class), any(), any())).thenThrow(ex);
        assertNull(azureBlobClient.download("file.txt", tempFile));
    }

    @Test
    void download_withString_shouldCallDownloadStream() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Mockito.doAnswer(invocation -> {
            ByteArrayOutputStream os = invocation.getArgument(0);
            os.write("test".getBytes());
            return null;
        }).when(blobClient).downloadStream(any(ByteArrayOutputStream.class));
        Mockito.when(blobContainerClient.getBlobClient(anyString())).thenReturn(blobClient);
        ByteArrayOutputStream result = azureBlobClient.download("file.txt");
        assertNotNull(result);
        assertEquals("test", result.toString());
        Mockito.verify(blobClient).downloadStream(any(ByteArrayOutputStream.class));
    }

    @Test
    void download_withString_blobStorageExceptionNot404_shouldThrow() {
        BlobStorageException ex = Mockito.mock(BlobStorageException.class);
        Mockito.when(ex.getStatusCode()).thenReturn(500);
        Mockito.doThrow(ex).when(blobClient).downloadStream(any(ByteArrayOutputStream.class));
        assertThrows(BlobStorageException.class, () -> azureBlobClient.download("file.txt"));
    }

    @Test
    void download_withString_blobStorageException404_shouldReturnNull() {
        BlobStorageException ex = Mockito.mock(BlobStorageException.class);
        Mockito.when(ex.getStatusCode()).thenReturn(404);
        Mockito.doThrow(ex).when(blobClient).downloadStream(any(ByteArrayOutputStream.class));
        assertNull(azureBlobClient.download("file.txt"));
    }
}


