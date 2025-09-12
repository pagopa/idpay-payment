package it.gov.pagopa.payment.service.payment;


import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.dto.PreviewPaymentDTO;
import it.gov.pagopa.payment.dto.barcode.AuthBarCodePaymentDTO;
import it.gov.pagopa.payment.dto.barcode.TransactionBarCodeCreationRequest;
import it.gov.pagopa.payment.dto.barcode.TransactionBarCodeResponse;
import it.gov.pagopa.payment.service.payment.barcode.BarCodeAuthPaymentService;
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
                                     BarCodeAuthPaymentService barCodeAuthPaymentService) {
        this.barCodeCreationService = barCodeCreationService;
        this.barCodeAuthPaymentService = barCodeAuthPaymentService;
    }

    @Override
    public TransactionBarCodeResponse createTransaction(TransactionBarCodeCreationRequest trxBRCodeCreationRequest, String userId) {
        return barCodeCreationService.createTransaction(
                trxBRCodeCreationRequest,
                RewardConstants.TRX_CHANNEL_BARCODE,
                userId);
    }

    @Override
    public AuthPaymentDTO authPayment(String trxCode, AuthBarCodePaymentDTO authBarCodePaymentDTO, String merchantId, String pointOfSaleId, String acquirerId) {
        return barCodeAuthPaymentService.authPayment(trxCode, authBarCodePaymentDTO, merchantId, pointOfSaleId, acquirerId);
    }

    @Override
    public PreviewPaymentDTO previewPayment(String productGtin, String trxCode, Long amountCents) {
        return barCodeAuthPaymentService.previewPayment(productGtin, trxCode, amountCents);
    }

}
