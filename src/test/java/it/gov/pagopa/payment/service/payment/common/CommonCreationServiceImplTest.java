package it.gov.pagopa.payment.service.payment.common;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.mongodb.client.result.UpdateResult;
import it.gov.pagopa.payment.exception.custom.TransactionInvalidException;
import it.gov.pagopa.payment.exception.custom.InitiativeInvalidException;
import it.gov.pagopa.payment.exception.custom.InitiativeNotfoundException;
import it.gov.pagopa.payment.connector.rest.merchant.MerchantConnector;
import it.gov.pagopa.payment.connector.rest.merchant.dto.MerchantDetailDTO;
import it.gov.pagopa.payment.constants.PaymentConstants;
import it.gov.pagopa.payment.dto.mapper.TransactionCreationRequest2TransactionInProgressMapper;
import it.gov.pagopa.payment.dto.mapper.TransactionInProgress2TransactionResponseMapper;
import it.gov.pagopa.payment.dto.qrcode.TransactionCreationRequest;
import it.gov.pagopa.payment.dto.qrcode.TransactionResponse;
import it.gov.pagopa.payment.enums.InitiativeRewardType;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.model.InitiativeConfig;
import it.gov.pagopa.payment.model.RewardRule;
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
import java.time.LocalDate;
import java.util.Optional;
import java.util.stream.Stream;
import org.bson.BsonString;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

@ExtendWith(MockitoExtension.class)
class CommonCreationServiceImplTest {

  public static final LocalDate TODAY = LocalDate.now();
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

  CommonCreationServiceImpl CommonCreationService;

  @BeforeEach
  void setUp() {
    CommonCreationService =
        new CommonCreationServiceImpl(
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

    when(rewardRuleRepository.findById("INITIATIVEID1")).thenReturn(Optional.of(buildRule("INITIATIVEID1", InitiativeRewardType.DISCOUNT)));
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
        CommonCreationService.createTransaction(
            trxCreationReq,
            RewardConstants.TRX_CHANNEL_QRCODE,
            "MERCHANTID1",
            "ACQUIRERID1",
                "IDTRXISSUER1");

    Assertions.assertNotNull(result);
    Assertions.assertEquals(trxCreated, result);
  }

  private RewardRule buildRule(String initiativeid, InitiativeRewardType initiativeRewardType) {
    return RewardRule.builder().id(initiativeid)
            .initiativeConfig(InitiativeConfig.builder()
                    .initiativeId(initiativeid)
                    .initiativeRewardType(initiativeRewardType)
                    .startDate(TODAY.minusDays(1))
                    .endDate(TODAY.plusDays(1))
                    .build())
            .build();
  }

  @Test
  void createTransactionTrxCodeHit() {

    TransactionCreationRequest trxCreationReq = TransactionCreationRequestFaker.mockInstance(1);
    MerchantDetailDTO merchantDetailDTO = MerchantDetailDTOFaker.mockInstance(1);
    TransactionResponse trxCreated = TransactionResponseFaker.mockInstance(1);
    TransactionInProgress trx = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.CREATED);

    when(rewardRuleRepository.findById("INITIATIVEID1")).thenReturn(Optional.of(buildRule("INITIATIVEID1", InitiativeRewardType.DISCOUNT)));
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
            CommonCreationService.createTransaction(
            trxCreationReq,
            RewardConstants.TRX_CHANNEL_QRCODE,
            "MERCHANTID1",
            "ACQUIRERID1",
            "IDTRXISSUER1");

    Assertions.assertNotNull(result);
    Assertions.assertEquals(trxCreated, result);
  }

  @Test
  void createTransaction_InitiativeNotFound() {

    TransactionCreationRequest trxCreationReq = TransactionCreationRequestFaker.mockInstance(1);

    when(rewardRuleRepository.findById("INITIATIVEID1")).thenReturn(Optional.empty());

    InitiativeNotfoundException result =
        Assertions.assertThrows(
            InitiativeNotfoundException.class,
            () ->
                    CommonCreationService.createTransaction(
                    trxCreationReq,
                    RewardConstants.TRX_CHANNEL_QRCODE,
                    "MERCHANTID1",
                    "ACQUIRERID1",
                    "IDTRXISSUER1"));

    Assertions.assertEquals(PaymentConstants.ExceptionCode.INITIATIVE_NOT_FOUND, result.getCode());
  }

  @Test
  void createTransaction_InitiativeNotDiscount() {

    TransactionCreationRequest trxCreationReq = TransactionCreationRequestFaker.mockInstance(1);

    when(rewardRuleRepository.findById("INITIATIVEID1")).thenReturn(Optional.of(buildRule("INITIATIVEID1", InitiativeRewardType.REFUND)));

    InitiativeNotfoundException result =
            Assertions.assertThrows(
                InitiativeNotfoundException.class,
                    () ->
                            CommonCreationService.createTransaction(
                                    trxCreationReq,
                                    RewardConstants.TRX_CHANNEL_QRCODE,
                                    "MERCHANTID1",
                                    "ACQUIRERID1",
                                    "IDTRXISSUER1"));

    Assertions.assertEquals(PaymentConstants.ExceptionCode.INITIATIVE_NOT_DISCOUNT, result.getCode());
  }

  @Test
  void createTransaction_AmountZero() {

    TransactionCreationRequest trxCreationReq = TransactionCreationRequestFaker.mockInstance(1);
    trxCreationReq.setAmountCents(0L);

    TransactionInvalidException result =
        Assertions.assertThrows(
            TransactionInvalidException.class,
            () ->
                    CommonCreationService.createTransaction(
                    trxCreationReq,
                    RewardConstants.TRX_CHANNEL_QRCODE,
                    "MERCHANTID1",
                    "ACQUIRERID1",
                    "IDTRXISSUER1"));

    Assertions.assertEquals(PaymentConstants.ExceptionCode.AMOUNT_NOT_VALID, result.getCode());
  }

  @ParameterizedTest
  @MethodSource("dateArguments")
  void createTransaction_InvalidDate(LocalDate invalidDate) {

    TransactionCreationRequest trxCreationReq = TransactionCreationRequestFaker.mockInstance(1);

    RewardRule rule = buildRuleWithInvalidDate(trxCreationReq, invalidDate);
    when(rewardRuleRepository.findById(trxCreationReq.getInitiativeId()))
            .thenReturn(Optional.of(rule));

    InitiativeInvalidException result =
        Assertions.assertThrows(
            InitiativeInvalidException.class,
            () ->
                    CommonCreationService.createTransaction(
                    trxCreationReq,
                    RewardConstants.TRX_CHANNEL_QRCODE,
                    "MERCHANTID1",
                    "ACQUIRERID1",
                    "IDTRXISSUER1"));

    Assertions.assertEquals(PaymentConstants.ExceptionCode.INITIATIVE_INVALID_DATE, result.getCode());
  }

  private static Stream<Arguments> dateArguments() {
     return Stream.of(
             Arguments.of(TODAY.plusDays(1)),
             Arguments.of(TODAY.minusDays(1))
     );
  }

  private RewardRule buildRuleWithInvalidDate(TransactionCreationRequest trxCreationReq, LocalDate invalidDate) {
    RewardRule rule = buildRule(trxCreationReq.getInitiativeId(), InitiativeRewardType.DISCOUNT);
    InitiativeConfig config = rule.getInitiativeConfig();

    if (invalidDate.isAfter(TODAY)) {
      config.setStartDate(invalidDate);
    } else {
      config.setEndDate(invalidDate);
    }
    rule.setInitiativeConfig(config);

    return rule;
  }
}
