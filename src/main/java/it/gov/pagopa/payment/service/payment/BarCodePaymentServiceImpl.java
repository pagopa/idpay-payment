package it.gov.pagopa.payment.service.payment;


import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.dto.brcode.TransactionBRCodeCreationRequest;
import it.gov.pagopa.payment.dto.brcode.TransactionBRCodeResponse;
import it.gov.pagopa.payment.service.payment.barcode.BarCodeAuthPaymentService;
import it.gov.pagopa.payment.dto.brcode.TransactionBarCodeCreationRequest;
import it.gov.pagopa.payment.dto.brcode.TransactionBarCodeResponse;
import it.gov.pagopa.payment.service.payment.barcode.BarCodeCreationService;
import it.gov.pagopa.payment.utils.RewardConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class BarCodePaymentServiceImpl implements BarCodePaymentService {
    private final BarCodeCreationService barCodeCreationService;
    private final BarCodeAuthPaymentService barCodeAuthPaymentService;

    public BarCodePaymentServiceImpl(BarCodeCreationService barCodeCreationService,
                                     BarCodeAuthPaymentService brCodeAuthPaymentService) {
        this.barCodeCreationService = barCodeCreationService;
        this.barCodeAuthPaymentService = brCodeAuthPaymentService;
    }

    @Override
    public TransactionBarCodeResponse createTransaction(TransactionBarCodeCreationRequest trxBRCodeCreationRequest, String userId) {
        return barCodeCreationService.createTransaction(
                trxBRCodeCreationRequest,
                RewardConstants.TRX_CHANNEL_BARCODE,
                userId);
    }

    @Override
    public AuthPaymentDTO authPayment(String trxCode, long amountCents, String merchantId) {
        return barCodeAuthPaymentService.authPayment(trxCode, merchantId, amountCents);
    }
}
