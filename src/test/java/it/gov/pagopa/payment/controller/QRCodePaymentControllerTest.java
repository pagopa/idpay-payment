package it.gov.pagopa.payment.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.payment.dto.qrcode.TransactionResponse;
import it.gov.pagopa.payment.dto.qrcode.TransactionCreationRequest;
import it.gov.pagopa.payment.exception.ClientExceptionWithBody;
import it.gov.pagopa.payment.service.QRCodePaymentService;
import it.gov.pagopa.payment.test.fakers.TransactionCreationRequestFaker;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(QRCodePaymentControllerImpl.class)
class QRCodePaymentControllerTest {

  @MockBean private QRCodePaymentService qrCodePaymentService;

  @Autowired MockMvc mockMvc;

  @Autowired ObjectMapper objectMapper;

  @Test
  void createTransactionSuccess() throws Exception {
    when(qrCodePaymentService.createTransaction(any(TransactionCreationRequest.class)))
        .thenReturn(new TransactionResponse());

    mockMvc
        .perform(
            post("/idpay/payment/qr-code/")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(TransactionCreationRequestFaker.mockInstance(1))))
        .andExpect(status().is2xxSuccessful())
        .andReturn();
  }

  @Test
  void createTransactionInitiativeNotFound() throws Exception {
    when(qrCodePaymentService.createTransaction(any(TransactionCreationRequest.class)))
        .thenThrow(
            new ClientExceptionWithBody(
                HttpStatus.NOT_FOUND,
                "NOT FOUND",
                "Cannot find initiative with ID: [%s]".formatted("test")));

    mockMvc
        .perform(
            post("/idpay/payment/qr-code/")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(TransactionCreationRequestFaker.mockInstance(1))))
        .andExpect(status().is4xxClientError())
        .andReturn();
  }
}
