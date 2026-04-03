package it.gov.pagopa.payment.service.payment.barcode;

import it.gov.pagopa.payment.connector.rest.wallet.WalletConnector;
import it.gov.pagopa.payment.connector.rest.wallet.dto.WalletDTO;
import it.gov.pagopa.payment.constants.PaymentConstants;
import it.gov.pagopa.payment.dto.barcode.TransactionBarCodeCreationRequest;
import it.gov.pagopa.payment.dto.barcode.TransactionBarCodeResponse;
import it.gov.pagopa.payment.dto.mapper.TransactionBarCodeCreationRequest2TransactionInProgressMapper;
import it.gov.pagopa.payment.dto.mapper.TransactionBarCodeInProgress2TransactionResponseMapper;
import it.gov.pagopa.payment.enums.InitiativeRewardType;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.exception.custom.BudgetExhaustedException;
import it.gov.pagopa.payment.exception.custom.InitiativeInvalidException;
import it.gov.pagopa.payment.exception.custom.InitiativeNotfoundException;
import it.gov.pagopa.payment.exception.custom.UserNotOnboardedException;
import it.gov.pagopa.payment.model.InitiativeConfig;
import it.gov.pagopa.payment.model.RewardRule;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.RewardRuleRepository;
import it.gov.pagopa.payment.service.payment.TransactionInProgressService;
import it.gov.pagopa.payment.test.fakers.TransactionBarCodeResponseFaker;
import it.gov.pagopa.payment.test.fakers.TransactionInProgressFaker;
import it.gov.pagopa.payment.test.fakers.WalletDTOFaker;
import it.gov.pagopa.payment.utils.AuditUtilities;
import it.gov.pagopa.payment.utils.RewardConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BarCodeCreationServiceImplTest {
    public static final Instant TODAY = Instant.now();
    @Mock private RewardRuleRepository rewardRuleRepository;
    @Mock private AuditUtilities auditUtilitiesMock;
    @Mock
    private TransactionBarCodeCreationRequest2TransactionInProgressMapper transactionBarCodeCreationRequest2TransactionInProgressMapper;
    @Mock
    private TransactionBarCodeInProgress2TransactionResponseMapper transactionBarCodeInProgress2TransactionResponseMapper;
    @Mock private WalletConnector walletConnector;
    @Mock private TransactionInProgressService transactionInProgressServiceMock;

    private static final String INITIATIVE_NAME = "INITIATIVE_NAME";

    BarCodeCreationServiceImpl barCodeCreationService;

    int authorizationExpirationMinutes = 5;
    int extendedAuthorizationExpirationMinutes = 14400;

    @BeforeEach
    void setUp() {

        barCodeCreationService =
                new BarCodeCreationServiceImpl(
                        rewardRuleRepository,
                        auditUtilitiesMock,
                        transactionBarCodeCreationRequest2TransactionInProgressMapper,
                        transactionBarCodeInProgress2TransactionResponseMapper,
                        walletConnector,
                        transactionInProgressServiceMock,
                        authorizationExpirationMinutes,
                        extendedAuthorizationExpirationMinutes,
                        Clock.fixed(Instant.parse("2026-04-03T10:00:00Z"), ZoneOffset.UTC)
                );
    }

    //region Create Transaction
    @Test
    void createTransaction() {

        TransactionBarCodeCreationRequest trxCreationReq = TransactionBarCodeCreationRequest.builder()
                .initiativeId("INITIATIVEID")
                .build();
        TransactionBarCodeResponse trxCreated = TransactionBarCodeResponseFaker.mockInstance(1);
        TransactionInProgress trx = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.CREATED);

        WalletDTO walletDTO = WalletDTOFaker.mockInstance(1, "REFUNDABLE");
        walletDTO.setAmountCents(1000L);

        when(walletConnector.getWallet("INITIATIVEID", "USERID")).thenReturn(walletDTO);
        when(rewardRuleRepository.findById("INITIATIVEID")).thenReturn(Optional.of(buildRule("INITIATIVEID", InitiativeRewardType.DISCOUNT)));
        when(transactionBarCodeCreationRequest2TransactionInProgressMapper.apply(
                any(TransactionBarCodeCreationRequest.class),
                eq(RewardConstants.TRX_CHANNEL_BARCODE),
                anyString(),
                anyString(),
                any(),
                eq(false),
                any()))
                .thenReturn(trx);
        when(transactionBarCodeInProgress2TransactionResponseMapper.apply(any(TransactionInProgress.class)))
                .thenReturn(trxCreated);

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
                        .initiativeName(INITIATIVE_NAME)
                        .startDate(TODAY.minus(1,ChronoUnit.DAYS))
                        .endDate(TODAY.plus(1,ChronoUnit.DAYS))
                        .build())
                .build();
    }

    @Test
    void createTransaction_InitiativeNotFound() {

        TransactionBarCodeCreationRequest trxCreationReq = TransactionBarCodeCreationRequest.builder()
                .initiativeId("INITIATIVEID")
                .build();

        WalletDTO walletDTO = WalletDTOFaker.mockInstance(1, "REFUNDABLE");
        walletDTO.setAmountCents(1000L);

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

    @ParameterizedTest
    @ValueSource(longs = {-100, 0})
    void createTransaction_UserBudgetExhausted(long budgetAmount) {

        TransactionBarCodeCreationRequest trxCreationReq = TransactionBarCodeCreationRequest.builder()
                .initiativeId("INITIATIVEID")
                .build();

        WalletDTO walletDTO = WalletDTOFaker.mockInstance(1, "REFUNDABLE");
        walletDTO.setAmountCents(budgetAmount);

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

        Assertions.assertEquals(String.format("Budget exhausted for the current user and initiative [%s]", trxCreationReq.getInitiativeId()), result.getMessage());
    }

    @Test
    void createTransaction_walletStatusUnsubscribed() {
        // Given
        TransactionBarCodeCreationRequest trxCreationReq = TransactionBarCodeCreationRequest.builder()
                .initiativeId("INITIATIVEID")
                .build();

        WalletDTO walletDTO = WalletDTOFaker.mockInstance(1, PaymentConstants.WALLET_STATUS_UNSUBSCRIBED);
        walletDTO.setAmountCents(1000L);

        when(rewardRuleRepository.findById("INITIATIVEID")).thenReturn(Optional.of(buildRule("INITIATIVEID", InitiativeRewardType.DISCOUNT)));
        when(walletConnector.getWallet("INITIATIVEID", "USERID")).thenReturn(walletDTO);

        // When
        UserNotOnboardedException result = Assertions.assertThrows(UserNotOnboardedException.class,
                () -> barCodeCreationService.createTransaction(trxCreationReq, RewardConstants.TRX_CHANNEL_BARCODE, "USERID"));

        // Then
        Assertions.assertEquals(PaymentConstants.ExceptionCode.USER_UNSUBSCRIBED, result.getCode());
    }

    @Test
    void createTransaction_UserNotOnboarded() {
        // Given
        TransactionBarCodeCreationRequest trxCreationReq = TransactionBarCodeCreationRequest.builder()
                .initiativeId("INITIATIVEID")
                .build();


        when(rewardRuleRepository.findById("INITIATIVEID")).thenReturn(Optional.of(buildRule("INITIATIVEID", InitiativeRewardType.DISCOUNT)));
        when(walletConnector.getWallet("INITIATIVEID", "USERID")).thenThrow(new UserNotOnboardedException(String.format("The current user is not onboarded on initiative [%s]", "INITIATIVEID"),true,null));

        // When
        UserNotOnboardedException result = Assertions.assertThrows(UserNotOnboardedException.class,
                () -> barCodeCreationService.createTransaction(trxCreationReq, RewardConstants.TRX_CHANNEL_BARCODE, "USERID"));

        // Then
        Assertions.assertEquals(PaymentConstants.ExceptionCode.USER_NOT_ONBOARDED, result.getCode());
    }

    @ParameterizedTest
    @MethodSource("dateArguments")
    void createTransaction_InvalidDate(Instant invalidDate) {

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
                Arguments.of(TODAY.plus(1,ChronoUnit.DAYS)),
                Arguments.of(TODAY.minus(1,ChronoUnit.DAYS))
        );
    }

    private RewardRule buildRuleWithInvalidDate(TransactionBarCodeCreationRequest trxCreationReq, Instant invalidDate) {
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

    //endregion

    //region Create extended transaction
    @Test
    void createExtendedTransaction() {

        TransactionBarCodeCreationRequest trxCreationReq = TransactionBarCodeCreationRequest.builder()
                .initiativeId("INITIATIVEID")
                .build();


        TransactionBarCodeResponse trxCreated = TransactionBarCodeResponseFaker.mockInstance(1);
        TransactionInProgress trx = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.CREATED);

        WalletDTO walletDTO = WalletDTOFaker.mockInstance(1, "REFUNDABLE");
        walletDTO.setAmountCents(1000L);

        when(rewardRuleRepository.findById("INITIATIVEID")).thenReturn(Optional.of(buildRule("INITIATIVEID", InitiativeRewardType.DISCOUNT)));
        when(transactionBarCodeCreationRequest2TransactionInProgressMapper.apply(
                any(TransactionBarCodeCreationRequest.class),
                eq(RewardConstants.TRX_CHANNEL_BARCODE),
                anyString(),
                anyString(),
                any(),
                eq(true),
                any()))
                .thenReturn(trx);
        when(transactionBarCodeInProgress2TransactionResponseMapper.apply(any(TransactionInProgress.class)))
                .thenReturn(trxCreated);

        TransactionBarCodeResponse result =
                barCodeCreationService.createExtendedTransaction(
                        trxCreationReq,
                        RewardConstants.TRX_CHANNEL_BARCODE,
                        "USERID");

        Assertions.assertNotNull(result);
        Assertions.assertEquals(trxCreated, result);
    }

    @Test
    void createExtendedTransaction_InitiativeNotFound() {

        TransactionBarCodeCreationRequest trxCreationReq = TransactionBarCodeCreationRequest.builder()
                .initiativeId("INITIATIVEID")
                .build();

        WalletDTO walletDTO = WalletDTOFaker.mockInstance(1, "REFUNDABLE");
        walletDTO.setAmountCents(1000L);

        when(rewardRuleRepository.findById("INITIATIVEID")).thenReturn(Optional.empty());

        InitiativeNotfoundException result =
                Assertions.assertThrows(
                        InitiativeNotfoundException.class,
                        () ->
                                barCodeCreationService.createExtendedTransaction(
                                        trxCreationReq,
                                        RewardConstants.TRX_CHANNEL_BARCODE,
                                        "USERID"));

        Assertions.assertEquals(PaymentConstants.ExceptionCode.INITIATIVE_NOT_FOUND, result.getCode());
    }

    @Test
    void createExtendedTransaction_InitiativeNotDiscount() {

        TransactionBarCodeCreationRequest trxCreationReq = TransactionBarCodeCreationRequest.builder()
                .initiativeId("INITIATIVEID")
                .build();

        when(rewardRuleRepository.findById("INITIATIVEID")).thenReturn(Optional.of(buildRule("INITIATIVEID", InitiativeRewardType.REFUND)));

        InitiativeNotfoundException result =
                Assertions.assertThrows(
                        InitiativeNotfoundException.class,
                        () ->
                                barCodeCreationService.createExtendedTransaction(
                                        trxCreationReq,
                                        RewardConstants.TRX_CHANNEL_BARCODE,
                                        "USERID"));

        Assertions.assertEquals(PaymentConstants.ExceptionCode.INITIATIVE_NOT_DISCOUNT, result.getCode());
    }

    @ParameterizedTest
    @MethodSource("dateArguments")
    void createExtendedTransaction_InvalidDate(Instant invalidDate) {

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
                                barCodeCreationService.createExtendedTransaction(
                                        trxCreationReq,
                                        RewardConstants.TRX_CHANNEL_BARCODE,
                                        "USERID"));

        Assertions.assertEquals(PaymentConstants.ExceptionCode.INITIATIVE_INVALID_DATE, result.getCode());
    }

    @ParameterizedTest
    @ValueSource(longs = {-100, -1})
    void createExtendedTransaction_UserBudgetExhausted(long voucherAmountCents) {

        TransactionBarCodeCreationRequest trxCreationReq = TransactionBarCodeCreationRequest.builder()
                .initiativeId("INITIATIVEID")
                .voucherAmountCents(voucherAmountCents)
                .build();

        when(rewardRuleRepository.findById("INITIATIVEID")).thenReturn(Optional.of(buildRule("INITIATIVEID", InitiativeRewardType.DISCOUNT)));

        BudgetExhaustedException result =
                Assertions.assertThrows(
                        BudgetExhaustedException.class,
                        () ->
                                barCodeCreationService.createExtendedTransaction(
                                        trxCreationReq,
                                        RewardConstants.TRX_CHANNEL_BARCODE,
                                        "USERID"));

        Assertions.assertEquals(String.format("Budget exhausted for the current user and initiative [%s]", trxCreationReq.getInitiativeId()), result.getMessage()); }
    @Test
    void shouldReturnTrxDatePlusAuthorizationMinutesWhenNotExtended()  {
        TransactionInProgress  trx =  new  TransactionInProgress();
        trx.setTrxDate(Instant.now());
        trx.setExtendedAuthorization(false);

        InitiativeConfig  initiative  = null;

        Instant  result =  barCodeCreationService.calculateTrxEndDate(trx,  initiative);

        Instant  expected = trx.getTrxDate().plus(authorizationExpirationMinutes,ChronoUnit.MINUTES);
        Assertions.assertEquals(expected, result);

    }

    @Test

    void shouldUseInitiativeEndDateWhenExtendedAndInitiativeEndDateNotNull() {

        Instant trxDate = Instant.parse("2026-04-01T13:15:30Z");
        Instant initiativeEndDate = Instant.parse("2026-04-02T00:00:00Z");

        TransactionInProgress trx = new TransactionInProgress();
        trx.setTrxDate(trxDate);
        trx.setExtendedAuthorization(true);

        InitiativeConfig initiative = new InitiativeConfig();
        initiative.setEndDate(initiativeEndDate);

        Instant expected = initiativeEndDate
                .atZone(ZoneId.of("Europe/Rome"))
                .toLocalDate()
                .atStartOfDay(ZoneId.of("Europe/Rome"))
                .toInstant()   // startOfDay(2026-04-02)
                .plus(1, ChronoUnit.DAYS)
                .minusNanos(1);  // fine giornata

        Instant result = barCodeCreationService.calculateTrxEndDate(trx, initiative);

        Assertions.assertEquals(expected, result);
    }


    @Test
    void shouldReturnTrxDatePlusExtendedAuthorizationMinutesWhenExtendedAndInitiativeEndDateNotNull()
    {

        Instant trxDate = Instant.parse("2026-04-01T10:15:00Z");
        Instant initiativeEndDate = Instant.parse("2026-04-11T00:00:00Z");

        TransactionInProgress trx = new TransactionInProgress();
        trx.setTrxDate(trxDate);
        trx.setExtendedAuthorization(true);

        InitiativeConfig initiative = new InitiativeConfig();
        initiative.setEndDate(initiativeEndDate);

        Instant result = barCodeCreationService.calculateTrxEndDate(trx, initiative);

        Instant expected = trxDate.plusSeconds(extendedAuthorizationExpirationMinutes * 60L);

        Assertions.assertEquals(expected, result);
    }

    @Test
    void shouldReturnTrxDatePlusExtendedAuthorizationMinutesWhenExtendedAndInitiativeNull() {

        Instant trxDate = Instant.parse("2026-04-10T10:00:00Z");

        TransactionInProgress trx = new TransactionInProgress();
        trx.setTrxDate(trxDate);
        trx.setExtendedAuthorization(true);

        InitiativeConfig initiative = null;

        Instant result = barCodeCreationService.calculateTrxEndDate(trx, initiative);

        Instant expected = trxDate.plus(extendedAuthorizationExpirationMinutes, ChronoUnit.MINUTES);

        Assertions.assertEquals(expected, result);
    }

    @Test
    void shouldReturnTrxDatePlusExtendedAuthorizationMinutesWhenExtendedAndInitiativeEndDateNull() {
        Instant trxDate = Instant.parse("2026-04-10T13:15:00Z");

        TransactionInProgress trx = new TransactionInProgress();
        trx.setTrxDate(trxDate);
        trx.setExtendedAuthorization(true);

        InitiativeConfig initiative = new InitiativeConfig();

        Instant result = barCodeCreationService.calculateTrxEndDate(trx, initiative);

        Instant expected = trxDate.plusSeconds(extendedAuthorizationExpirationMinutes * 60L);

        Assertions.assertEquals(expected, result);
    }
}