package it.gov.pagopa.payment.controller;

import it.gov.pagopa.payment.dto.qrcode.SyncTrxStatusDTO;
import it.gov.pagopa.payment.test.fakers.SyncTrxStatusFaker;
import org.junit.jupiter.api.Test;

class QRCodePaymentControllerImplTest {


    private static final SyncTrxStatusDTO trxStatus_1= SyncTrxStatusFaker.mockInstance(1);
    private static final SyncTrxStatusDTO trxStatus_2=SyncTrxStatusFaker.mockInstance(2);
    private static final SyncTrxStatusDTO trxStatus_3=SyncTrxStatusFaker.mockInstance(3);


    @Test
    void getStatusTransaction() {

    }
}