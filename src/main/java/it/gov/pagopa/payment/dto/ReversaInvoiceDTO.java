package it.gov.pagopa.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class ReversaInvoiceDTO {

    @NotNull
    private MultipartFile file;

    @NotBlank
    @Size(max = 255)
    private String fileName;

    @NotBlank
    private String type;

}
