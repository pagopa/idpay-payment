package it.gov.pagopa.payment.service.qrcode;

import com.mongodb.client.result.UpdateResult;
import it.gov.pagopa.common.web.exception.ClientException;
import it.gov.pagopa.common.web.exception.ClientExceptionWithBody;
import it.gov.pagopa.payment.connector.rest.merchant.MerchantConnector;
import it.gov.pagopa.payment.connector.rest.merchant.dto.MerchantDetailDTO;
import it.gov.pagopa.payment.dto.mapper.TransactionCreationRequest2TransactionInProgressMapper;
import it.gov.pagopa.payment.dto.mapper.TransactionInProgress2TransactionResponseMapper;
import it.gov.pagopa.payment.dto.qrcode.TransactionCreationRequest;
import it.gov.pagopa.payment.dto.qrcode.TransactionResponse;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.RewardRuleRepository;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.test.fakers.MerchantDetailDTOFaker;
import it.gov.pagopa.payment.test.fakers.TransactionCreationRequestFaker;
import it.gov.pagopa.payment.test.fakers.TransactionInProgressFaker;
import it.gov.pagopa.payment.test.fakers.TransactionResponseFaker;
import it.gov.pagopa.payment.utils.AuditUtilities;
import it.gov.pagopa.payment.utils.RewardConstants;
import it.gov.pagopa.payment.utils.TrxCodeGenUtil;
import org.bson.BsonString;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.springframework.http.HttpStatus;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QRCodeCreationServiceTest {

  @Mock
  private TransactionInProgress2TransactionResponseMapper transactionInProgress2TransactionResponseMapper;
  @Mock
  private TransactionCreationRequest2TransactionInProgressMapper
      transactionCreationRequest2TransactionInProgressMapper;

  @Mock private RewardRuleRepository rewardRuleRepository;
  @Mock private TransactionInProgressRepository transactionInProgressRepository;
  @Mock private TrxCodeGenUtil trxCodeGenUtil;
  @Mock private AuditUtilities auditUtilitiesMock;
  @Mock private MerchantConnector merchantConnectorMock;

  QRCodeCreationService qrCodeCreationService;

  @BeforeEach
  void setUp() {
    qrCodeCreationService =
        new QRCodeCreationServiceImpl(
            transactionInProgress2TransactionResponseMapper,
            transactionCreationRequest2TransactionInProgressMapper,
            rewardRuleRepository,
            transactionInProgressRepository,
            trxCodeGenUtil,
            auditUtilitiesMock,
            merchantConnectorMock);
  }

  @Test
  void createTransaction() {

    TransactionCreationRequest trxCreationReq = TransactionCreationRequestFaker.mockInstance(1);
    MerchantDetailDTO merchantDetailDTO = MerchantDetailDTOFaker.mockInstance(1);
    TransactionResponse trxCreated = TransactionResponseFaker.mockInstance(1);
    TransactionInProgress trx = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.CREATED);

    when(rewardRuleRepository.existsById("INITIATIVEID1")).thenReturn(true);
    when(merchantConnectorMock.merchantDetail("MERCHANTID1","INITIATIVEID1")).thenReturn(merchantDetailDTO);
    when(transactionCreationRequest2TransactionInProgressMapper.apply(
            any(TransactionCreationRequest.class),
            eq(RewardConstants.TRX_CHANNEL_QRCODE),
            anyString(),
            anyString(),
            any(MerchantDetailDTO.class),
            anyString()))
        .thenReturn(trx);
    when(transactionInProgress2TransactionResponseMapper.apply(any(TransactionInProgress.class)))
        .thenReturn(trxCreated);
    when(trxCodeGenUtil.get()).thenReturn("trxcode1");
    when(transactionInProgressRepository.createIfExists(trx, "trxcode1"))
        .thenReturn(UpdateResult.acknowledged(0L, 0L, new BsonString(trx.getId())));

    TransactionResponse result =
        qrCodeCreationService.createTransaction(
            trxCreationReq,
            RewardConstants.TRX_CHANNEL_QRCODE,
            "MERCHANTID1",
            "ACQUIRERID1",
                "IDTRXISSUER1");

    Assertions.assertNotNull(result);
    Assertions.assertEquals(trxCreated, result);
  }

  @Test
  void createTransactionTrxCodeHit() {

    TransactionCreationRequest trxCreationReq = TransactionCreationRequestFaker.mockInstance(1);
    MerchantDetailDTO merchantDetailDTO = MerchantDetailDTOFaker.mockInstance(1);
    TransactionResponse trxCreated = TransactionResponseFaker.mockInstance(1);
    TransactionInProgress trx = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.CREATED);

    when(rewardRuleRepository.existsById("INITIATIVEID1")).thenReturn(true);
    when(merchantConnectorMock.merchantDetail("MERCHANTID1","INITIATIVEID1")).thenReturn(merchantDetailDTO);
    when(transactionCreationRequest2TransactionInProgressMapper.apply(
            any(TransactionCreationRequest.class),
            eq(RewardConstants.TRX_CHANNEL_QRCODE),
            anyString(),
            anyString(),
            any(MerchantDetailDTO.class),
            anyString()))
        .thenReturn(trx);
    when(transactionInProgress2TransactionResponseMapper.apply(any(TransactionInProgress.class)))
        .thenReturn(trxCreated);
    when(trxCodeGenUtil.get())
        .thenAnswer(
            new Answer<String>() {
              private int count = 0;

              public String answer(InvocationOnMock invocation) {
                return "trxcode%d".formatted(++count);
              }
            });
    when(transactionInProgressRepository.createIfExists(trx, "trxcode1"))
        .thenReturn(UpdateResult.acknowledged(1L, 0L, null));
    when(transactionInProgressRepository.createIfExists(trx, "trxcode2"))
        .thenReturn(UpdateResult.acknowledged(0L, 0L, new BsonString(trx.getId())));

    TransactionResponse result =
        qrCodeCreationService.createTransaction(
            trxCreationReq,
            RewardConstants.TRX_CHANNEL_QRCODE,
            "MERCHANTID1",
            "ACQUIRERID1",
            "IDTRXISSUER1");

    Assertions.assertNotNull(result);
    Assertions.assertEquals(trxCreated, result);
  }

  @Test
  void createTransactionNotFound() {

    TransactionCreationRequest trxCreationReq = TransactionCreationRequestFaker.mockInstance(1);

    when(rewardRuleRepository.existsById("INITIATIVEID1")).thenReturn(false);

    ClientException result =
        Assertions.assertThrows(
            ClientException.class,
            () ->
                qrCodeCreationService.createTransaction(
                    trxCreationReq,
                    RewardConstants.TRX_CHANNEL_QRCODE,
                    "MERCHANTID1",
                    "ACQUIRERID1",
                    "IDTRXISSUER1"));

    Assertions.assertEquals(HttpStatus.NOT_FOUND, result.getHttpStatus());
    Assertions.assertEquals("NOT FOUND", ((ClientExceptionWithBody) result).getCode());
  }

  @Test
  void createTransactionAmountZero() {

    TransactionCreationRequest trxCreationReq = TransactionCreationRequestFaker.mockInstance(1);
    trxCreationReq.setAmountCents(0L);

    when(rewardRuleRepository.existsById("INITIATIVEID1")).thenReturn(true);

    ClientException result =
        Assertions.assertThrows(
            ClientException.class,
            () ->
                qrCodeCreationService.createTransaction(
                    trxCreationReq,
                    RewardConstants.TRX_CHANNEL_QRCODE,
                    "MERCHANTID1",
                    "ACQUIRERID1",
                    "IDTRXISSUER1"));

    Assertions.assertEquals(HttpStatus.BAD_REQUEST, result.getHttpStatus());
    Assertions.assertEquals("INVALID AMOUNT", ((ClientExceptionWithBody) result).getCode());
  }
}
