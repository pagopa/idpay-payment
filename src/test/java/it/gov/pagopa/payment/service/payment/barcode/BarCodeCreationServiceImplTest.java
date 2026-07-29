package it.gov.pagopa.payment.service.payment.barcode;

import it.gov.pagopa.payment.connector.rest.wallet.WalletConnector;
import it.gov.pagopa.payment.connector.rest.wallet.dto.WalletDTO;
import it.gov.pagopa.payment.constants.PaymentConstants;
import it.gov.pagopa.payment.dto.barcode.TransactionBarCodeCreationRequest;
import it.gov.pagopa.payment.dto.barcode.TransactionBarCodeResponse;
import it.gov.pagopa.payment.dto.mapper.TransactionMapper;
import it.gov.pagopa.payment.entity.Transaction;
import it.gov.pagopa.payment.enums.InitiativeRewardType;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.exception.custom.BudgetExhaustedException;
import it.gov.pagopa.payment.exception.custom.InitiativeInvalidException;
import it.gov.pagopa.payment.exception.custom.InitiativeNotfoundException;
import it.gov.pagopa.payment.exception.custom.UserNotOnboardedException;
import it.gov.pagopa.payment.model.InitiativeConfig;
import it.gov.pagopa.payment.model.RewardRule;
import it.gov.pagopa.payment.repository.RewardRuleRepository;
import it.gov.pagopa.payment.service.payment.TransactionService;
import it.gov.pagopa.payment.test.fakers.TransactionBarCodeResponseFaker;
import it.gov.pagopa.payment.test.fakers.TransactionFaker;
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

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BarCodeCreationServiceImplTest {
    public static final LocalDate TODAY = LocalDate.now(ZoneId.of("Europe/Rome"));
    @Mock
    private RewardRuleRepository rewardRuleRepository;
    @Mock
    private AuditUtilities auditUtilitiesMock;
    @Mock
    private TransactionMapper transactionMapper;
    @Mock
    private WalletConnector walletConnector;
    @Mock
    private TransactionService transactionServiceMock;

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
                        transactionMapper,
                        walletConnector,
                        transactionServiceMock,
                        authorizationExpirationMinutes,
                        extendedAuthorizationExpirationMinutes);
    }

    @Test
    void createTransaction() {
        TransactionBarCodeCreationRequest trxCreationReq = TransactionBarCodeCreationRequest.builder()
                .initiativeId("INITIATIVEID")
                .build();
        TransactionBarCodeResponse trxCreated = TransactionBarCodeResponseFaker.mockInstance(1);
        Transaction trx = TransactionFaker.mockInstance(1, SyncTrxStatus.CREATED);

        WalletDTO walletDTO = WalletDTOFaker.mockInstance(1, "REFUNDABLE");
        walletDTO.setAmountCents(1000L);
        walletDTO.setInitialAmountCents(1000L);

        when(walletConnector.getWallet("INITIATIVEID", "USERID")).thenReturn(walletDTO);
        when(rewardRuleRepository.findById("INITIATIVEID")).thenReturn(Optional.of(buildRule("INITIATIVEID", InitiativeRewardType.DISCOUNT)));
        when(transactionMapper.transactionBarCodeCreationRequestToTransaction(
                any(TransactionBarCodeCreationRequest.class),
                eq(RewardConstants.TRX_CHANNEL_BARCODE),
                anyString(),
                anyString(),
                anyMap(),
                eq(false),
                isNull()))
                .thenReturn(trx);
        when(transactionMapper.transactionToTransactionBarCodeResponse(any(Transaction.class)))
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
                        .startDate(TODAY.minusDays(1))
                        .endDate(TODAY.plusDays(1))
                        .build())
                .build();
    }

    @Test
    void createTransaction_InitiativeNotFound() {
        TransactionBarCodeCreationRequest trxCreationReq = TransactionBarCodeCreationRequest.builder()
                .initiativeId("INITIATIVEID")
                .build();

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
        walletDTO.setInitialAmountCents(1000L);

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
        TransactionBarCodeCreationRequest trxCreationReq = TransactionBarCodeCreationRequest.builder()
                .initiativeId("INITIATIVEID")
                .build();

        WalletDTO walletDTO = WalletDTOFaker.mockInstance(1, PaymentConstants.WALLET_STATUS_UNSUBSCRIBED);
        walletDTO.setAmountCents(1000L);
        walletDTO.setInitialAmountCents(1000L);

        when(rewardRuleRepository.findById("INITIATIVEID")).thenReturn(Optional.of(buildRule("INITIATIVEID", InitiativeRewardType.DISCOUNT)));
        when(walletConnector.getWallet("INITIATIVEID", "USERID")).thenReturn(walletDTO);

        UserNotOnboardedException result = Assertions.assertThrows(UserNotOnboardedException.class,
                () -> barCodeCreationService.createTransaction(trxCreationReq, RewardConstants.TRX_CHANNEL_BARCODE, "USERID"));

        Assertions.assertEquals(PaymentConstants.ExceptionCode.USER_UNSUBSCRIBED, result.getCode());
    }

    @Test
    void createTransaction_UserNotOnboarded() {
        TransactionBarCodeCreationRequest trxCreationReq = TransactionBarCodeCreationRequest.builder()
                .initiativeId("INITIATIVEID")
                .build();

        when(rewardRuleRepository.findById("INITIATIVEID")).thenReturn(Optional.of(buildRule("INITIATIVEID", InitiativeRewardType.DISCOUNT)));
        when(walletConnector.getWallet("INITIATIVEID", "USERID")).thenThrow(new UserNotOnboardedException(String.format("The current user is not onboarded on initiative [%s]", "INITIATIVEID"), true, null));

        UserNotOnboardedException result = Assertions.assertThrows(UserNotOnboardedException.class,
                () -> barCodeCreationService.createTransaction(trxCreationReq, RewardConstants.TRX_CHANNEL_BARCODE, "USERID"));

        Assertions.assertEquals(PaymentConstants.ExceptionCode.USER_NOT_ONBOARDED, result.getCode());
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

    @Test
    void createExtendedTransaction() {
        TransactionBarCodeCreationRequest trxCreationReq = TransactionBarCodeCreationRequest.builder()
                .initiativeId("INITIATIVEID")
                .voucherAmountCents(1000L)
                .build();

        TransactionBarCodeResponse trxCreated = TransactionBarCodeResponseFaker.mockInstance(1);
        Transaction trx = TransactionFaker.mockInstance(1, SyncTrxStatus.CREATED);
        trx.setExtendedAuthorization(true);

        when(rewardRuleRepository.findById("INITIATIVEID")).thenReturn(Optional.of(buildRule("INITIATIVEID", InitiativeRewardType.DISCOUNT)));
        when(transactionMapper.transactionBarCodeCreationRequestToTransaction(
                any(TransactionBarCodeCreationRequest.class),
                eq(RewardConstants.TRX_CHANNEL_BARCODE),
                anyString(),
                anyString(),
                anyMap(),
                eq(true),
                isNull()))
                .thenReturn(trx);
        when(transactionMapper.transactionToTransactionBarCodeResponse(any(Transaction.class)))
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
    void createExtendedTransaction_InvalidDate(LocalDate invalidDate) {
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

        Assertions.assertEquals(String.format("Budget exhausted for the current user and initiative [%s]", trxCreationReq.getInitiativeId()), result.getMessage());
    }

    @Test
    void shouldReturnTrxDatePlusAuthorizationMinutesWhenNotExtended() {
        Transaction trx = new Transaction();
        trx.setTrxDate(OffsetDateTime.now(ZoneId.of("Europe/Rome")));
        trx.setExtendedAuthorization(false);

        InitiativeConfig initiative = null;

        OffsetDateTime result = barCodeCreationService.calculateTrxEndDate(trx, initiative);

        OffsetDateTime expected = trx.getTrxDate().plusMinutes(authorizationExpirationMinutes);
        Assertions.assertEquals(expected, result);
    }

    @Test
    void shouldUseInitiativeEndDateWhenExtendedAndInitiativeEndDateNotNull() {
        Transaction trx = new Transaction();
        trx.setTrxDate(OffsetDateTime.now(ZoneId.of("Europe/Rome")));
        trx.setExtendedAuthorization(true);

        InitiativeConfig initiative = new InitiativeConfig();
        LocalDate initiativeEndDate = LocalDate.now(ZoneId.of("Europe/Rome")).plusDays(1);
        initiative.setEndDate(initiativeEndDate);

        OffsetDateTime offsetEndDate = initiativeEndDate.atStartOfDay(ZoneId.of("Europe/Rome")).toOffsetDateTime();
        OffsetDateTime result = barCodeCreationService.calculateTrxEndDate(trx, initiative);

        OffsetDateTime expected = offsetEndDate
                .truncatedTo(ChronoUnit.DAYS).plusDays(1).minusNanos(1);

        Assertions.assertEquals(expected, result);
    }

    @Test
    void shouldReturnTrxDatePlusExtendedAuthorizationMinutesWhenExtendedAndInitiativeEndDateNotNull() {
        Transaction trx = new Transaction();
        trx.setTrxDate(OffsetDateTime.now(ZoneId.of("Europe/Rome")));
        trx.setExtendedAuthorization(true);

        InitiativeConfig initiative = new InitiativeConfig();
        LocalDate initiativeEndDate = LocalDate.now(ZoneId.of("Europe/Rome")).plusDays(10);
        initiative.setEndDate(initiativeEndDate);

        OffsetDateTime result = barCodeCreationService.calculateTrxEndDate(trx, initiative);

        OffsetDateTime expected = trx.getTrxDate().plusMinutes(extendedAuthorizationExpirationMinutes)
                .truncatedTo(ChronoUnit.DAYS).plusDays(1).minusNanos(1);

        Assertions.assertEquals(expected, result);
    }

    @Test
    void shouldReturnTrxDatePlusExtendedAuthorizationMinutesWhenExtendedAndInitiativeNull() {
        Transaction trx = new Transaction();
        trx.setTrxDate(OffsetDateTime.now(ZoneId.of("Europe/Rome")));
        trx.setExtendedAuthorization(true);

        InitiativeConfig initiative = null;

        OffsetDateTime result = barCodeCreationService.calculateTrxEndDate(trx, initiative);

        OffsetDateTime expected = trx.getTrxDate().plusMinutes(extendedAuthorizationExpirationMinutes)
                .truncatedTo(ChronoUnit.DAYS).plusDays(1).minusNanos(1);

        Assertions.assertEquals(expected, result);
    }

    @Test
    void shouldReturnTrxDatePlusExtendedAuthorizationMinutesWhenExtendedAndInitiativeEndDateNull() {
        Transaction trx = new Transaction();
        trx.setTrxDate(OffsetDateTime.now(ZoneId.of("Europe/Rome")));
        trx.setExtendedAuthorization(true);

        InitiativeConfig initiative = new InitiativeConfig();

        OffsetDateTime result = barCodeCreationService.calculateTrxEndDate(trx, initiative);

        OffsetDateTime expected = trx.getTrxDate().plusMinutes(extendedAuthorizationExpirationMinutes)
                .truncatedTo(ChronoUnit.DAYS).plusDays(1).minusNanos(1);

        Assertions.assertEquals(expected, result);
    }
}
