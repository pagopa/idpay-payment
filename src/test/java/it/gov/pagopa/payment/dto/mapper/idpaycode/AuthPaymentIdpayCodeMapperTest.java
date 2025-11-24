package it.gov.pagopa.payment.dto.mapper.idpaycode;

import it.gov.pagopa.common.utils.TestUtils;
import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.dto.idpaycode.AuthPaymentIdpayCodeDTO;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.test.fakers.AuthPaymentDTOFaker;
import it.gov.pagopa.payment.test.fakers.TransactionInProgressFaker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class AuthPaymentIdpayCodeMapperTest {
    private final AuthPaymentIdpayCodeMapper authPaymentIdpayCodeMapper = new AuthPaymentIdpayCodeMapper();

    @Test
    void authPaymentMapper() {
        TransactionInProgress trx = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.IDENTIFIED);
        AuthPaymentDTO authPayment = AuthPaymentDTOFaker.mockInstance(1, trx);
        String secondFactor = "secondFactor";

        AuthPaymentIdpayCodeDTO result = authPaymentIdpayCodeMapper.authPaymentMapper(authPayment, secondFactor);


        Assertions.assertNotNull(result);
        Assertions.assertEquals(authPayment.getId(), result.getId());
        Assertions.assertEquals(authPayment.getRewardCents(), result.getRewardCents());
        Assertions.assertEquals(authPayment.getInitiativeId(), result.getInitiativeId());
        Assertions.assertEquals(authPayment.getInitiativeName(), result.getInitiativeName());
        Assertions.assertEquals(authPayment.getBusinessName(), result.getBusinessName());
        Assertions.assertEquals(authPayment.getRejectionReasons(), result.getRejectionReasons());
        Assertions.assertEquals(authPayment.getStatus(), result.getStatus());
        Assertions.assertEquals(authPayment.getTrxCode(), result.getTrxCode());
        Assertions.assertEquals(authPayment.getTrxDate(), result.getTrxDate());
        Assertions.assertEquals(authPayment.getAmountCents(), result.getAmountCents());
        Assertions.assertEquals(authPayment.getCounters(), result.getCounters());
        Assertions.assertEquals(authPayment.getRewards(), result.getRewards());
        Assertions.assertEquals(secondFactor, result.getSecondFactor());

        TestUtils.checkNotNullFields(result, "splitPayment",
                "residualAmountCents");
    }
}