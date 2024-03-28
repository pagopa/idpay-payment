package it.gov.pagopa.payment.service.payment;

import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.dto.PinBlockDTO;
import it.gov.pagopa.payment.dto.idpaycode.RelateUserResponse;
import it.gov.pagopa.payment.dto.mapper.idpaycode.RelateUserResponseMapper;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.service.payment.idpaycode.IdpayCodeAuthPaymentService;
import it.gov.pagopa.payment.service.payment.idpaycode.IdpayCodePreviewService;
import it.gov.pagopa.payment.service.payment.idpaycode.IdpayCodeRelateUserService;
import it.gov.pagopa.payment.test.fakers.AuthPaymentDTOFaker;
import it.gov.pagopa.payment.test.fakers.PinBlockDTOFaker;
import it.gov.pagopa.payment.test.fakers.TransactionInProgressFaker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdpayCodePaymentServiceTest {
    private static final String MERCHANTID = "MERCHANTID";
    private static final String FISCALCODE = "FISCALCODE";

    @Mock private IdpayCodeRelateUserService idpayCodeRelateUserServiceMock;
    @Mock private IdpayCodePreviewService idpayCodePreviewServiceMock;
    @Mock private IdpayCodeAuthPaymentService idpayCodeAuthPaymentServiceMock;
    private IdpayCodePaymentService idpayCodePaymentService;

    @BeforeEach
    void setUp(){
        idpayCodePaymentService = new IdpayCodePaymentServiceImpl(idpayCodeRelateUserServiceMock, idpayCodePreviewServiceMock, idpayCodeAuthPaymentServiceMock);
    }

    @Test
    void relateUser() {
        //Given
        TransactionInProgress trx = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.IDENTIFIED);
        RelateUserResponseMapper mapper = new RelateUserResponseMapper();

        RelateUserResponse resultUserResponse = mapper.transactionMapper(trx);
        when(idpayCodeRelateUserServiceMock.relateUser(trx.getId(), FISCALCODE))
                .thenReturn(resultUserResponse);
        //When
        RelateUserResponse result = idpayCodePaymentService.relateUser(trx.getId(), FISCALCODE);

        //Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(resultUserResponse, result);
    }

    @Test
    void previewPayment() {
        //Given
        TransactionInProgress trx = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.IDENTIFIED);
        AuthPaymentDTO preview = AuthPaymentDTOFaker.mockInstance(1,trx);
        when(idpayCodePreviewServiceMock.previewPayment(trx.getId(), MERCHANTID))
                .thenReturn(preview);
        //When
        AuthPaymentDTO result = idpayCodePaymentService.previewPayment(trx.getId(), MERCHANTID);

        //Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(preview, result);
    }

    @Test
    void authPayment() {
        //Given
        TransactionInProgress trx = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.AUTHORIZED);
        AuthPaymentDTO authPaymentDTO = AuthPaymentDTOFaker.mockInstance(1,trx);
        authPaymentDTO.setStatus(SyncTrxStatus.AUTHORIZED);
        PinBlockDTO pinBlockDTO = PinBlockDTOFaker.mockInstance();

        when(idpayCodeAuthPaymentServiceMock.authPayment(trx.getId(), MERCHANTID,pinBlockDTO))
                .thenReturn(authPaymentDTO);
        //When
        AuthPaymentDTO result = idpayCodePaymentService.authPayment(trx.getId(), MERCHANTID,pinBlockDTO);

        //Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(authPaymentDTO, result);
    }



}