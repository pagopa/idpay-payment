package it.gov.pagopa.payment.service.payment.barcode;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.mongodb.client.result.UpdateResult;
import it.gov.pagopa.common.web.exception.custom.forbidden.BudgetExhaustedException;
import it.gov.pagopa.common.web.exception.custom.forbidden.InitiativeInvalidException;
import it.gov.pagopa.common.web.exception.custom.forbidden.UserNotOnboardedException;
import it.gov.pagopa.common.web.exception.custom.notfound.InitiativeNotfoundException;
import it.gov.pagopa.payment.connector.rest.merchant.MerchantConnector;
import it.gov.pagopa.payment.connector.rest.wallet.WalletConnector;
import it.gov.pagopa.payment.connector.rest.wallet.dto.WalletDTO;
import it.gov.pagopa.payment.constants.PaymentConstants;
import it.gov.pagopa.payment.dto.barcode.TransactionBarCodeCreationRequest;
import it.gov.pagopa.payment.dto.barcode.TransactionBarCodeResponse;
import it.gov.pagopa.payment.dto.mapper.TransactionBarCodeCreationRequest2TransactionInProgressMapper;
import it.gov.pagopa.payment.dto.mapper.TransactionBarCodeInProgress2TransactionResponseMapper;
import it.gov.pagopa.payment.dto.mapper.TransactionCreationRequest2TransactionInProgressMapper;
import it.gov.pagopa.payment.dto.mapper.TransactionInProgress2TransactionResponseMapper;
import it.gov.pagopa.payment.enums.InitiativeRewardType;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.model.InitiativeConfig;
import it.gov.pagopa.payment.model.RewardRule;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.RewardRuleRepository;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.test.fakers.TransactionBarCodeResponseFaker;
import it.gov.pagopa.payment.test.fakers.TransactionInProgressFaker;
import it.gov.pagopa.payment.test.fakers.WalletDTOFaker;
import it.gov.pagopa.payment.utils.AuditUtilities;
import it.gov.pagopa.payment.utils.RewardConstants;
import it.gov.pagopa.payment.utils.TrxCodeGenUtil;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.stream.Stream;
import org.bson.BsonString;
import org.jetbrains.annotations.NotNull;
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
class BarCodeCreationServiceImplTest {
    public static final LocalDate TODAY = LocalDate.now();
    @Mock
    private TransactionInProgress2TransactionResponseMapper transactionInProgress2TransactionResponseMapper;
    @Mock
    private TransactionBarCodeCreationRequest2TransactionInProgressMapper transactionBarCodeCreationRequest2TransactionInProgressMapper;
    @Mock
    private TransactionCreationRequest2TransactionInProgressMapper transactionCreationRequest2TransactionInProgressMapper;
    @Mock
    private TransactionBarCodeInProgress2TransactionResponseMapper transactionBarCodeInProgress2TransactionResponseMapper;
    @Mock private RewardRuleRepository rewardRuleRepository;
    @Mock private TransactionInProgressRepository transactionInProgressRepository;
    @Mock private TrxCodeGenUtil trxCodeGenUtil;
    @Mock private AuditUtilities auditUtilitiesMock;
    @Mock private MerchantConnector merchantConnector;
    @Mock private WalletConnector walletConnector;

    BarCodeCreationServiceImpl barCodeCreationService;

    @BeforeEach
    void setUp() {
        barCodeCreationService =
                new BarCodeCreationServiceImpl(transactionInProgress2TransactionResponseMapper,
                        transactionCreationRequest2TransactionInProgressMapper,
                        rewardRuleRepository,
                        transactionInProgressRepository,
                        trxCodeGenUtil,
                        auditUtilitiesMock,
                        merchantConnector,
                        transactionBarCodeCreationRequest2TransactionInProgressMapper,
                        transactionBarCodeInProgress2TransactionResponseMapper,
                        walletConnector);
    }

    @Test
    void createTransaction() {

        TransactionBarCodeCreationRequest trxCreationReq = TransactionBarCodeCreationRequest.builder()
                .initiativeId("INITIATIVEID")
                .build();
        TransactionBarCodeResponse trxCreated = TransactionBarCodeResponseFaker.mockInstance(1);
        TransactionInProgress trx = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.CREATED);

        WalletDTO walletDTO = WalletDTOFaker.mockInstance(1, "REFUNDABLE");
        walletDTO.setAmount(BigDecimal.TEN);

        when(walletConnector.getWallet("INITIATIVEID", "USERID")).thenReturn(walletDTO);
        when(rewardRuleRepository.findById("INITIATIVEID")).thenReturn(Optional.of(buildRule("INITIATIVEID", InitiativeRewardType.DISCOUNT)));
        when(transactionBarCodeCreationRequest2TransactionInProgressMapper.apply(
                any(TransactionBarCodeCreationRequest.class),
                eq(RewardConstants.TRX_CHANNEL_BARCODE),
                anyString()))
                .thenReturn(trx);
        when(transactionBarCodeInProgress2TransactionResponseMapper.apply(any(TransactionInProgress.class)))
                .thenReturn(trxCreated);
        when(trxCodeGenUtil.get()).thenReturn("trxcode1");
        when(transactionInProgressRepository.createIfExists(trx, "trxcode1"))
                .thenReturn(UpdateResult.acknowledged(0L, 0L, new BsonString(trx.getId())));

        TransactionBarCodeResponse result =
                barCodeCreationService.createTransaction(
                        trxCreationReq,
                        RewardConstants.TRX_CHANNEL_BARCODE,
                        "USERID");

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

        TransactionBarCodeCreationRequest trxCreationReq = TransactionBarCodeCreationRequest.builder()
                .initiativeId("INITIATIVEID")
                .build();
        TransactionBarCodeResponse trxCreated = TransactionBarCodeResponseFaker.mockInstance(1);
        TransactionInProgress trx = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.CREATED);

        WalletDTO walletDTO = WalletDTOFaker.mockInstance(1, "REFUNDABLE");
        walletDTO.setAmount(BigDecimal.TEN);

        when(walletConnector.getWallet("INITIATIVEID", "USERID")).thenReturn(walletDTO);

        when(rewardRuleRepository.findById("INITIATIVEID")).thenReturn(Optional.of(buildRule("INITIATIVEID", InitiativeRewardType.DISCOUNT)));
        when(transactionBarCodeCreationRequest2TransactionInProgressMapper.apply(
                any(TransactionBarCodeCreationRequest.class),
                eq(RewardConstants.TRX_CHANNEL_BARCODE),
                anyString()))
                .thenReturn(trx);
        when(transactionBarCodeInProgress2TransactionResponseMapper.apply(any(TransactionInProgress.class)))
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

        TransactionBarCodeResponse result =
                barCodeCreationService.createTransaction(
                        trxCreationReq,
                        RewardConstants.TRX_CHANNEL_BARCODE,
                        "USERID");

        Assertions.assertNotNull(result);
        Assertions.assertEquals(trxCreated, result);
    }

    @Test
    void createTransaction_InitiativeNotFound() {

        TransactionBarCodeCreationRequest trxCreationReq = TransactionBarCodeCreationRequest.builder()
                .initiativeId("INITIATIVEID")
                .build();

        WalletDTO walletDTO = WalletDTOFaker.mockInstance(1, "REFUNDABLE");
        walletDTO.setAmount(BigDecimal.TEN);

        when(rewardRuleRepository.findById("INITIATIVEID")).thenReturn(Optional.empty());

        InitiativeNotfoundException result =
                Assertions.assertThrows(
                    InitiativeNotfoundException.class,
                        () ->
                                barCodeCreationService.createTransaction(
                                        trxCreationReq,
                                        RewardConstants.TRX_CHANNEL_BARCODE,
                                        "USERID"));

        Assertions.assertEquals(PaymentConstants.ExceptionCode.INITIATIVE_NOT_FOUND, result.getCode());
    }

    @Test
    void createTransaction_InitiativeNotDiscount() {

        TransactionBarCodeCreationRequest trxCreationReq = TransactionBarCodeCreationRequest.builder()
                .initiativeId("INITIATIVEID")
                .build();

        when(rewardRuleRepository.findById("INITIATIVEID")).thenReturn(Optional.of(buildRule("INITIATIVEID", InitiativeRewardType.REFUND)));

        InitiativeNotfoundException result =
                Assertions.assertThrows(
                    InitiativeNotfoundException.class,
                        () ->
                                barCodeCreationService.createTransaction(
                                        trxCreationReq,
                                        RewardConstants.TRX_CHANNEL_BARCODE,
                                        "USERID"));

        Assertions.assertEquals(PaymentConstants.ExceptionCode.INITIATIVE_NOT_DISCOUNT, result.getCode());
    }

    @Test
    void createTransaction_UserBudgetExhausted() {

        TransactionBarCodeCreationRequest trxCreationReq = TransactionBarCodeCreationRequest.builder()
                .initiativeId("INITIATIVEID")
                .build();

        WalletDTO walletDTO = WalletDTOFaker.mockInstance(1, "REFUNDABLE");
        walletDTO.setAmount(BigDecimal.ZERO);

        when(rewardRuleRepository.findById("INITIATIVEID")).thenReturn(Optional.of(buildRule("INITIATIVEID", InitiativeRewardType.DISCOUNT)));
        when(walletConnector.getWallet("INITIATIVEID", "USERID")).thenReturn(walletDTO);

        BudgetExhaustedException result =
                Assertions.assertThrows(
                    BudgetExhaustedException.class,
                        () ->
                                barCodeCreationService.createTransaction(
                                        trxCreationReq,
                                        RewardConstants.TRX_CHANNEL_BARCODE,
                                        "USERID"));

        Assertions.assertEquals(String.format("The budget related to the user on initiativeId [%s] was exhausted.", trxCreationReq.getInitiativeId()), result.getMessage());
    }

    @Test
    void createTransaction_walletStatusUnsubscribed() {
        // Given
        TransactionBarCodeCreationRequest trxCreationReq = TransactionBarCodeCreationRequest.builder()
                .initiativeId("INITIATIVEID")
                .build();

        WalletDTO walletDTO = WalletDTOFaker.mockInstance(1, PaymentConstants.WALLET_STATUS_UNSUBSCRIBED);
        walletDTO.setAmount(BigDecimal.TEN);

        when(rewardRuleRepository.findById("INITIATIVEID")).thenReturn(Optional.of(buildRule("INITIATIVEID", InitiativeRewardType.DISCOUNT)));
        when(walletConnector.getWallet("INITIATIVEID", "USERID")).thenReturn(walletDTO);

        // When
        UserNotOnboardedException result = Assertions.assertThrows(UserNotOnboardedException.class,
                () -> barCodeCreationService.createTransaction(trxCreationReq, RewardConstants.TRX_CHANNEL_BARCODE, "USERID"));

        // Then
        Assertions.assertEquals(PaymentConstants.ExceptionCode.USER_UNSUBSCRIBED, result.getCode());
    }

    @ParameterizedTest
    @MethodSource("dateArguments")
    void createTransaction_InvalidDate(LocalDate invalidDate) {

        TransactionBarCodeCreationRequest trxCreationReq = TransactionBarCodeCreationRequest.builder()
                .initiativeId("INITIATIVEID")
                .build();

        RewardRule rule = buildRuleWithInvalidDate(trxCreationReq, invalidDate);
        when(rewardRuleRepository.findById(trxCreationReq.getInitiativeId()))
                .thenReturn(Optional.of(rule));

        InitiativeInvalidException result =
                Assertions.assertThrows(
                    InitiativeInvalidException.class,
                        () ->
                                barCodeCreationService.createTransaction(
                                        trxCreationReq,
                                        RewardConstants.TRX_CHANNEL_BARCODE,
                                        "USERID"));

        Assertions.assertEquals(PaymentConstants.ExceptionCode.INITIATIVE_INVALID_DATE, result.getCode());
    }

    private static Stream<Arguments> dateArguments() {
        return Stream.of(
                Arguments.of(TODAY.plusDays(1)),
                Arguments.of(TODAY.minusDays(1))
        );
    }

    @NotNull
    private RewardRule buildRuleWithInvalidDate(TransactionBarCodeCreationRequest trxCreationReq, LocalDate invalidDate) {
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