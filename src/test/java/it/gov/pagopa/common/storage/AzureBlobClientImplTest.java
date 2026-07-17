package it.gov.pagopa.common.storage;

import com.azure.core.http.rest.PagedIterable;
import com.azure.core.http.rest.Response;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.*;
import com.azure.storage.blob.options.BlobDownloadToFileOptions;
import com.azure.storage.blob.options.BlobParallelUploadOptions;
import com.azure.storage.blob.options.BlobUploadFromFileOptions;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;
import it.gov.pagopa.common.web.exception.ClientException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

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
    @Mock
    private BlobServiceClient blobServiceClient;
    @Mock
    private UserDelegationKey userDelegationKey;

    private AzureBlobClientImpl azureBlobClient;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(blobContainerClient.getBlobClient(anyString())).thenReturn(blobClient);
        azureBlobClient = new AzureBlobClientImpl(blobContainerClient, blobServiceClient, 3600);
    }

    @Test
    void uploadFile_shouldCallUploadFromFileWithResponse() {
        File file = new File("test.txt");
        when(blobClient.uploadFromFileWithResponse(any(BlobUploadFromFileOptions.class), any(), any())).thenReturn(blockBlobItemResponse);
        Response<BlockBlobItem> response = azureBlobClient.uploadFile(file, "dest", "text/plain");
        assertEquals(blockBlobItemResponse, response);
        verify(blobClient).uploadFromFileWithResponse(any(BlobUploadFromFileOptions.class), any(), any());
    }

    @Test
    void upload_shouldCallUploadWithResponse() {
        InputStream is = new ByteArrayInputStream("test".getBytes());
        when(blobClient.uploadWithResponse(any(BlobParallelUploadOptions.class), any(), any())).thenReturn(blockBlobItemResponse);
        Response<BlockBlobItem> response = azureBlobClient.upload(is, "dest", "text/plain");
        assertEquals(blockBlobItemResponse, response);
        verify(blobClient).uploadWithResponse(any(BlobParallelUploadOptions.class), any(), any());
    }

    @Test
    void deleteFile_shouldCallDeleteIfExistsWithResponse() {
        when(blobClient.deleteIfExistsWithResponse(any(), any(), any(), any())).thenReturn(booleanResponse);
        Response<Boolean> response = azureBlobClient.deleteFile("dest");
        assertEquals(booleanResponse, response);
        verify(blobClient).deleteIfExistsWithResponse(any(), any(), any(), any());
    }

    @Test
    void listFiles_shouldCallListBlobsByHierarchy() {
        when(blobContainerClient.listBlobsByHierarchy(anyString())).thenReturn(pagedIterable);
        PagedIterable<BlobItem> result = azureBlobClient.listFiles("path");
        assertEquals(pagedIterable, result);
        verify(blobContainerClient).listBlobsByHierarchy("path");
    }

    @Test
    void download_withPath_shouldCallDownloadToFileWithResponse() {
        Path tempFile = Paths.get(System.getProperty("java.io.tmpdir"), "test.txt");
        when(blobClient.downloadToFileWithResponse(any(BlobDownloadToFileOptions.class), any(), any())).thenReturn(blobPropertiesResponse);
        Response<BlobProperties> response = azureBlobClient.download("file.txt", tempFile);
        assertEquals(blobPropertiesResponse, response);
        verify(blobClient).downloadToFileWithResponse(any(BlobDownloadToFileOptions.class), any(), any());
    }

    @Test
    void download_withPath_blobStorageExceptionNot404_shouldThrow() {
        Path tempFile = Paths.get(System.getProperty("java.io.tmpdir"), "test.txt");
        BlobStorageException ex = mock(BlobStorageException.class);
        when(ex.getStatusCode()).thenReturn(500);
        when(blobClient.downloadToFileWithResponse(any(BlobDownloadToFileOptions.class), any(), any())).thenThrow(ex);
        assertThrows(BlobStorageException.class, () -> azureBlobClient.download("file.txt", tempFile));
    }

    @Test
    void download_withPath_blobStorageException404_shouldReturnNull() {
        Path tempFile = Paths.get(System.getProperty("java.io.tmpdir"), "test.txt");
        BlobStorageException ex = mock(BlobStorageException.class);
        when(ex.getStatusCode()).thenReturn(404);
        when(blobClient.downloadToFileWithResponse(any(BlobDownloadToFileOptions.class), any(), any())).thenThrow(ex);
        assertNull(azureBlobClient.download("file.txt", tempFile));
    }

    @Test
    void download_withString_shouldCallDownloadStream() {
        doAnswer(invocation -> {
            ByteArrayOutputStream os = invocation.getArgument(0);
            os.write("test".getBytes());
            return null;
        }).when(blobClient).downloadStream(any(ByteArrayOutputStream.class));
        when(blobContainerClient.getBlobClient(anyString())).thenReturn(blobClient);
        ByteArrayOutputStream result = azureBlobClient.download("file.txt");
        assertNotNull(result);
        assertEquals("test", result.toString());
        verify(blobClient).downloadStream(any(ByteArrayOutputStream.class));
    }

    @Test
    void download_withString_blobStorageExceptionNot404_shouldThrow() {
        BlobStorageException ex = mock(BlobStorageException.class);
        when(ex.getStatusCode()).thenReturn(500);
        doThrow(ex).when(blobClient).downloadStream(any(ByteArrayOutputStream.class));
        assertThrows(BlobStorageException.class, () -> azureBlobClient.download("file.txt"));
    }

    @Test
    void download_withString_blobStorageException404_shouldReturnNull() {
        BlobStorageException ex = mock(BlobStorageException.class);
        when(ex.getStatusCode()).thenReturn(404);
        doThrow(ex).when(blobClient).downloadStream(any(ByteArrayOutputStream.class));
        assertNull(azureBlobClient.download("file.txt"));
    }

    @Test
    void getInvoiceFileSignedUrl_shouldReturnSignedUrlWithValidToken() {
        String blobPath = "invoices/2024/invoice-123.pdf";
        String blobUrl = "https://storage.blob.core.windows.net/container/invoices/2024/invoice-123.pdf";
        String sasToken = "sv=2021-06-08&ss=bfqt&srt=sco&sp=rwdlac&se=2024-12-31T23:59:59Z";
        
        when(blobServiceClient.getUserDelegationKey(eq(null), any(OffsetDateTime.class)))
                .thenReturn(userDelegationKey);
        when(blobClient.generateUserDelegationSas(any(BlobServiceSasSignatureValues.class), any(UserDelegationKey.class)))
                .thenReturn(sasToken);
        when(blobClient.getBlobUrl()).thenReturn(blobUrl);
        
        String result = azureBlobClient.getInvoiceFileSignedUrl(blobPath);
        
        assertNotNull(result);
        assertTrue(result.contains(blobUrl));
        assertTrue(result.contains(sasToken));
        assertTrue(result.contains("?"));
        verify(blobServiceClient).getUserDelegationKey(eq(null), any(OffsetDateTime.class));
        verify(blobClient).generateUserDelegationSas(any(BlobServiceSasSignatureValues.class), any(UserDelegationKey.class));
    }

    @Test
    void getInvoiceFileSignedUrl_shouldUseCorrectSasDuration() {
        String blobPath = "invoices/2024/invoice-456.pdf";
        String sasToken = "sv=2021-06-08&ss=bfqt&srt=sco&sp=rwdlac";
        
        when(blobServiceClient.getUserDelegationKey(eq(null), any(OffsetDateTime.class)))
                .thenReturn(userDelegationKey);
        when(blobClient.generateUserDelegationSas(any(BlobServiceSasSignatureValues.class), any(UserDelegationKey.class)))
                .thenReturn(sasToken);
        when(blobClient.getBlobUrl()).thenReturn("https://storage.blob.core.windows.net/container/invoices/2024/invoice-456.pdf");
        
        azureBlobClient.getInvoiceFileSignedUrl(blobPath);
        
        ArgumentCaptor<OffsetDateTime> dateTimeCaptor = ArgumentCaptor.forClass(OffsetDateTime.class);
        verify(blobServiceClient).getUserDelegationKey(eq(null), dateTimeCaptor.capture());
        
        OffsetDateTime capturedDateTime = dateTimeCaptor.getValue();
        assertNotNull(capturedDateTime);
    }

    @Test
    void getInvoiceFileSignedUrl_shouldSetReadPermissionOnSasToken() {
        String blobPath = "invoices/2024/invoice-789.pdf";
        String sasToken = "sv=2021-06-08&ss=bfqt&srt=sco&sp=rwdlac";
        
        when(blobServiceClient.getUserDelegationKey(eq(null), any(OffsetDateTime.class)))
                .thenReturn(userDelegationKey);
        when(blobClient.generateUserDelegationSas(any(BlobServiceSasSignatureValues.class), any(UserDelegationKey.class)))
                .thenReturn(sasToken);
        when(blobClient.getBlobUrl()).thenReturn("https://storage.blob.core.windows.net/container/invoices/2024/invoice-789.pdf");
        
        azureBlobClient.getInvoiceFileSignedUrl(blobPath);
        
        ArgumentCaptor<BlobServiceSasSignatureValues> sasValuesCaptor = ArgumentCaptor.forClass(BlobServiceSasSignatureValues.class);
        verify(blobClient).generateUserDelegationSas(sasValuesCaptor.capture(), any(UserDelegationKey.class));
        
        BlobServiceSasSignatureValues capturedValues = sasValuesCaptor.getValue();
        assertNotNull(capturedValues);
    }

    @Test
    void getInvoiceFileSignedUrl_shouldThrowClientExceptionOnBlobStorageException() {
        String blobPath = "invoices/2024/invoice-error.pdf";
        BlobStorageException blobStorageException = mock(BlobStorageException.class);
        
        when(blobServiceClient.getUserDelegationKey(eq(null), any(OffsetDateTime.class)))
                .thenReturn(userDelegationKey);
        when(blobClient.generateUserDelegationSas(any(BlobServiceSasSignatureValues.class), any(UserDelegationKey.class)))
                .thenThrow(blobStorageException);
        
        assertThrows(ClientException.class, () -> azureBlobClient.getInvoiceFileSignedUrl(blobPath));
        verify(blobClient).generateUserDelegationSas(any(BlobServiceSasSignatureValues.class), any(UserDelegationKey.class));
    }

    @Test
    void getInvoiceFileSignedUrl_shouldGetCorrectBlobClient() {
        String blobPath = "invoices/2024/invoice-specific.pdf";
        String sasToken = "sv=2021-06-08&ss=bfqt&srt=sco&sp=rwdlac";
        
        when(blobServiceClient.getUserDelegationKey(eq(null), any(OffsetDateTime.class)))
                .thenReturn(userDelegationKey);
        when(blobClient.generateUserDelegationSas(any(BlobServiceSasSignatureValues.class), any(UserDelegationKey.class)))
                .thenReturn(sasToken);
        when(blobClient.getBlobUrl()).thenReturn("https://storage.blob.core.windows.net/container/invoices/2024/invoice-specific.pdf");
        
        azureBlobClient.getInvoiceFileSignedUrl(blobPath);
        
        verify(blobContainerClient).getBlobClient(blobPath);
    }

    @Test
    void getInvoiceFileSignedUrl_withEncodedUrl_shouldDecodeBeforeJoin() {
        String blobPath = "invoices/2024/invoice%20special.pdf";
        String encodedBlobUrl = "https://storage.blob.core.windows.net/container/invoices/2024/invoice%20special.pdf";
        String sasToken = "sv=2021-06-08&ss=bfqt&srt=sco&sp=rwdlac";
        
        when(blobServiceClient.getUserDelegationKey(eq(null), any(OffsetDateTime.class)))
                .thenReturn(userDelegationKey);
        when(blobClient.generateUserDelegationSas(any(BlobServiceSasSignatureValues.class), any(UserDelegationKey.class)))
                .thenReturn(sasToken);
        when(blobClient.getBlobUrl()).thenReturn(encodedBlobUrl);
        
        String result = azureBlobClient.getInvoiceFileSignedUrl(blobPath);
        
        assertNotNull(result);
        assertTrue(result.contains("?"));
        assertTrue(result.contains(sasToken));
    }
}


