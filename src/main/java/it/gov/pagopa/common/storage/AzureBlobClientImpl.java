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
import com.azure.storage.blob.sas.BlobSasPermission;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;
import it.gov.pagopa.common.web.exception.ClientException;
import it.gov.pagopa.payment.utils.Utilities;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.OffsetDateTime;
import java.util.Set;

import static it.gov.pagopa.payment.constants.PaymentConstants.ExceptionCode.ERROR_ON_GET_FILE_URL_REQUEST;

@Slf4j
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class AzureBlobClientImpl implements AzureBlobClient {

    private final BlobContainerClient blobContainerClient;
    private final BlobServiceClient blobServiceClient;
    protected final Integer sasDurationSeconds;

    protected AzureBlobClientImpl(BlobContainerClient blobContainerClient, BlobServiceClient blobServiceClient, Integer sasDurationSeconds) {
        this.blobContainerClient = blobContainerClient;
        this.blobServiceClient = blobServiceClient;
        this.sasDurationSeconds = sasDurationSeconds;
    }

    @Override
    public String getInvoiceFileSignedUrl(String blobPath) {
        OffsetDateTime expiryTime = OffsetDateTime.now().plusSeconds(sasDurationSeconds);
        UserDelegationKey userDelegationKey =
                blobServiceClient.getUserDelegationKey(null, expiryTime);

        BlobSasPermission sasPermission = new BlobSasPermission().setReadPermission(true);
        BlobClient blobClient = blobContainerClient.getBlobClient(blobPath);
        BlobServiceSasSignatureValues sasValues = new BlobServiceSasSignatureValues(expiryTime, sasPermission);

        try {
            String sasToken = blobClient.generateUserDelegationSas(sasValues, userDelegationKey);
            return StringUtils.joinWith("?",
                    URLDecoder.decode(blobClient.getBlobUrl(), StandardCharsets.UTF_8),
                    sasToken);
        } catch (BlobStorageException e) {
            log.error("Error generating SAS token");
            throw new ClientException(HttpStatus.INTERNAL_SERVER_ERROR, ERROR_ON_GET_FILE_URL_REQUEST, e);
        }
    }


    @Override
    public Response<BlockBlobItem> uploadFile(File file, String destination, String contentType) {
        log.info("Uploading file {} (contentType={}) into azure blob at destination {}", file.getName(), contentType, destination);

        return blobContainerClient.getBlobClient(destination)
                .uploadFromFileWithResponse(new BlobUploadFromFileOptions(file.getPath()), null, null);
    }

    @Override
    public Response<BlockBlobItem> upload(InputStream inputStream, String destination, String contentType) {
        log.info("Uploading (contentType={}) into azure blob at destination {}", Utilities.sanitizeString(contentType), Utilities.sanitizeString(destination));

        return blobContainerClient.getBlobClient(destination)
                .uploadWithResponse(new BlobParallelUploadOptions(inputStream), null, null);
    }

    @Override
    public Response<Boolean> deleteFile(String destination) {
        log.info("Deleting file {} from azure blob container", destination);

        return blobContainerClient.getBlobClient(destination)
                .deleteIfExistsWithResponse(DeleteSnapshotsOptionType.INCLUDE, null, null, null);
    }

    @Override
    public PagedIterable<BlobItem> listFiles(String path) {
        return blobContainerClient.listBlobsByHierarchy(path);
    }

    @Override
    public Response<BlobProperties> download(String filePath, Path destination) {
        log.info("Downloading file {} from azure blob container", filePath);

        createDirectoryIfNotExists(destination);

        try {
            return blobContainerClient.getBlobClient(filePath)
                    .downloadToFileWithResponse(new BlobDownloadToFileOptions(destination.toString())
                                    // override options
                                    .setOpenOptions(Set.of(
                                            StandardOpenOption.CREATE,
                                            StandardOpenOption.TRUNCATE_EXISTING,
                                            StandardOpenOption.READ,
                                            StandardOpenOption.WRITE)),
                            null, null
                    );
        } catch (BlobStorageException e) {
            if(e.getStatusCode()!=404){
                throw e;
            } else {
                return null;
            }
        }
    }

    @Override
    public ByteArrayOutputStream download(String filePath) {
        log.info("Downloading file {} from azure blob container", filePath);

        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            blobContainerClient.getBlobClient(filePath)
                    .downloadStream(outputStream);
            return outputStream;
        } catch (BlobStorageException e) {
            if(e.getStatusCode()!=404){
                throw e;
            } else {
                return null;
            }
        }
    }

    private static void createDirectoryIfNotExists(Path localFile) {
        Path directory = localFile.getParent();
        if (!Files.exists(directory)) {
            try {
                Files.createDirectories(directory);
            } catch (IOException e) {
                throw new IllegalStateException("Cannot create directory to store downloaded zip %s".formatted(localFile), e);
            }
        }
    }
}
