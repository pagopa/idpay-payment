package it.gov.pagopa.payment.service.payment;


import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.dto.PreviewPaymentDTO;
import it.gov.pagopa.payment.dto.barcode.AuthBarCodePaymentDTO;
import it.gov.pagopa.payment.dto.barcode.TransactionBarCodeCreationRequest;
import it.gov.pagopa.payment.dto.barcode.TransactionBarCodeEnrichedResponse;
import it.gov.pagopa.payment.dto.barcode.TransactionBarCodeResponse;
import it.gov.pagopa.payment.service.payment.barcode.BarCodeAuthPaymentService;
import it.gov.pagopa.payment.service.payment.barcode.BarCodeCaptureService;
import it.gov.pagopa.payment.service.payment.barcode.BarCodeCreationService;
import it.gov.pagopa.payment.service.payment.barcode.RetrieveActiveBarcode;
import it.gov.pagopa.payment.utils.RewardConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class BarCodePaymentServiceImpl implements BarCodePaymentService {
    private final BarCodeCreationService barCodeCreationService;
    private final BarCodeCaptureService barCodeCaptureService;
    private final BarCodeAuthPaymentService barCodeAuthPaymentService;
    private final RetrieveActiveBarcode retrieveActiveBarcode;

    public BarCodePaymentServiceImpl(BarCodeCreationService barCodeCreationService,
                                     BarCodeCaptureService barCodeCaptureService,
                                     BarCodeAuthPaymentService barCodeAuthPaymentService,
                                     RetrieveActiveBarcode retrieveActiveBarcode) {
        this.barCodeCreationService = barCodeCreationService;
        this.barCodeCaptureService = barCodeCaptureService;
        this.barCodeAuthPaymentService = barCodeAuthPaymentService;
        this.retrieveActiveBarcode = retrieveActiveBarcode;
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

    @Override
    public TransactionBarCodeResponse findOldestNotAuthorized(String userId, String initiativeId) {
        return retrieveActiveBarcode.findOldestNotAuthorized(userId, initiativeId);
    }

    @Override
    public TransactionBarCodeResponse capturePayment(String trxCode){
        return barCodeCaptureService.capturePayment(trxCode);
    }

    @Override
    public TransactionBarCodeEnrichedResponse createExtendedTransaction(TransactionBarCodeCreationRequest trxBRCodeCreationRequest, String userId) {
        return barCodeCreationService.createExtendedTransaction(
                trxBRCodeCreationRequest,
                RewardConstants.TRX_CHANNEL_BARCODE,
                userId);
    }
}
