package it.gov.pagopa.payment.service.payment.qrcode;

import it.gov.pagopa.common.utils.TestUtils;
import it.gov.pagopa.common.web.exception.ClientException;
import it.gov.pagopa.common.web.exception.ClientExceptionWithBody;
import it.gov.pagopa.payment.constants.PaymentConstants;
import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.service.payment.common.CommonPreAuthService;
import it.gov.pagopa.payment.test.fakers.AuthPaymentDTOFaker;
import it.gov.pagopa.payment.test.fakers.TransactionInProgressFaker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QRCodePreAuthServiceImplTest {

  @Mock private TransactionInProgressRepository transactionInProgressRepositoryMock;
  @Mock private CommonPreAuthService commonPreAuthServiceMock;

  private QRCodePreAuthService qrCodePreAuthService;

  private static final String USER_ID1 = "USERID1";

  @BeforeEach
  void setUp() {
    long authorizationExpirationMinutes = 4350;
    qrCodePreAuthService =
            new QRCodePreAuthServiceImpl(
                    transactionInProgressRepositoryMock,
                    commonPreAuthServiceMock);
  }

  @Test
  void relateUser() {
    //Given
    TransactionInProgress trx = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.CREATED);

    when(transactionInProgressRepositoryMock.findByTrxCode(anyString()))
            .thenReturn(Optional.of(trx));

    trx.setUserId(USER_ID1);
    when(commonPreAuthServiceMock.relateUser(trx, trx.getUserId()))
            .thenReturn(trx);

    AuthPaymentDTO authPaymentDTO = AuthPaymentDTOFaker.mockInstance(1, trx);
    when(commonPreAuthServiceMock.previewPayment(trx))
            .thenReturn(authPaymentDTO);


    AuthPaymentDTO result = qrCodePreAuthService.relateUser("trxcode1", USER_ID1);

    Assertions.assertNotNull(result);
    TestUtils.checkNotNullFields(result, "rejectionReasons");

    verify(transactionInProgressRepositoryMock, times(1)).findByTrxCode(anyString());
    verify(commonPreAuthServiceMock, times(1)).relateUser(any(), anyString());
    verify(commonPreAuthServiceMock, times(1)).previewPayment(any());
  }


  @Test
  void relateUserTrxNotFound() {

    ClientException result = Assertions.assertThrows(ClientException.class, () ->
        qrCodePreAuthService.relateUser("trxcode1", "USERID1")
    );

    Assertions.assertNotNull(result);
    Assertions.assertEquals(HttpStatus.NOT_FOUND, result.getHttpStatus());

    verify(transactionInProgressRepositoryMock, times(1)).findByTrxCode(anyString());
    verify(commonPreAuthServiceMock, times(0)).relateUser(any(), anyString());
    verify(commonPreAuthServiceMock, times(0)).previewPayment(any());
  }

  @Test
  void relateUserOtherException() {
    TransactionInProgress trx = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.CREATED);
    String trxCode = trx.getTrxCode();
    Mockito.when(transactionInProgressRepositoryMock.findByTrxCode(trxCode))
            .thenReturn(Optional.of(trx));

    Mockito.when(commonPreAuthServiceMock.relateUser(trx, USER_ID1))
            .thenThrow(new ClientExceptionWithBody(
            HttpStatus.INTERNAL_SERVER_ERROR,
            PaymentConstants.ExceptionCode.GENERIC_ERROR,
            "DUMMY_MESSAGE"));

    ClientExceptionWithBody result = Assertions.assertThrows(ClientExceptionWithBody.class, () ->
            qrCodePreAuthService.relateUser(trxCode, USER_ID1));

    Assertions.assertNotNull(result);
    Assertions.assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result.getHttpStatus());
    Assertions.assertEquals(PaymentConstants.ExceptionCode.GENERIC_ERROR, result.getCode());

  }

}
