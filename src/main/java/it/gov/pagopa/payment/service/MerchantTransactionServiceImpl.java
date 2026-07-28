package it.gov.pagopa.payment.service;

import it.gov.pagopa.common.utils.CommonUtilities;
import it.gov.pagopa.payment.connector.decrypt.DecryptRestConnector;
import it.gov.pagopa.payment.connector.encrypt.EncryptRestConnector;
import it.gov.pagopa.payment.dto.CFDTO;
import it.gov.pagopa.payment.dto.MerchantTransactionDTO;
import it.gov.pagopa.payment.dto.MerchantTransactionsListDTO;
import it.gov.pagopa.payment.dto.TrxFiltersDTO;
import it.gov.pagopa.payment.dto.mapper.TransactionMapper;
import it.gov.pagopa.payment.entity.Transaction;
import it.gov.pagopa.payment.enums.RewardBatchTrxStatus;
import it.gov.pagopa.payment.exception.custom.PDVInvocationException;
import it.gov.pagopa.payment.exception.custom.TransactionMissingParametersException;
import it.gov.pagopa.payment.model.InvoiceData;
import it.gov.pagopa.payment.repository.TransactionRepository;
import it.gov.pagopa.payment.service.payment.TransactionService;
import it.gov.pagopa.payment.utils.CommonPaymentUtilities;
import it.gov.pagopa.payment.utils.RewardConstants;
import it.gov.pagopa.payment.utils.TransactionSpecifications;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.*;

import static it.gov.pagopa.payment.constants.PaymentConstants.ExceptionCode.STATUS_NOT_ALLOWED;
import static it.gov.pagopa.payment.constants.PaymentConstants.ExceptionMessage.STATUS_NOT_ALLOWED_MESSAGE;

@Service
public class MerchantTransactionServiceImpl implements MerchantTransactionService {

    private static final String DEFAULT_PROCESSED_SORT_FIELD = "rewardBatchStatusTrx";
    private static final String TO_CHECK_STATUS = "TO_CHECK";
    private static final Set<String> EXCLUDED_OPERATORS = Set.of("operator1", "operator2", "operator3");
    private static final String MISSING_VALUE_PLACEHOLDER = "-";

    private final int authorizationExpirationMinutes;
    private final TransactionRepository transactionRepository;
    private final DecryptRestConnector decryptRestConnector;
    private final EncryptRestConnector encryptRestConnector;
    private final TransactionService transactionService;
    private final TransactionMapper transactionMapper;

    public MerchantTransactionServiceImpl(
            @Value("${app.common.expirations.authorizationMinutes}") int authorizationExpirationMinutes,
            TransactionRepository transactionRepository,
            DecryptRestConnector decryptRestConnector,
            EncryptRestConnector encryptRestConnector,
            TransactionService transactionService,
            TransactionMapper transactionMapper) {
        this.authorizationExpirationMinutes = authorizationExpirationMinutes;
        this.transactionRepository = transactionRepository;
        this.decryptRestConnector = decryptRestConnector;
        this.encryptRestConnector = encryptRestConnector;
        this.transactionService = transactionService;
        this.transactionMapper = transactionMapper;
    }

    @Override
    public MerchantTransactionsListDTO getMerchantTransactions(
            String merchantId,
            String initiativeId,
            String fiscalCode,
            String status,
            Pageable pageable) {
        String userId = StringUtils.isNotBlank(fiscalCode) ? encryptCF(fiscalCode) : null;

        Specification<Transaction> spec = TransactionSpecifications.withFilters(
                merchantId,
                null, // pointOfSaleId
                initiativeId,
                userId,
                status,
                null, // productGtin
                null  // trxCode
        );

        Page<Transaction> entityPage = transactionRepository.findAll(spec, pageable);

        List<MerchantTransactionDTO> merchantTransactions = entityPage.getContent().stream()
                .map(this::populateMerchantTransactionDTO)
                .toList();

        Page<MerchantTransactionDTO> dtoPage = entityPage.map(this::populateMerchantTransactionDTO);

        return toMerchantTransactionsListDTO(merchantTransactions, dtoPage);
    }

    @Override
    public MerchantTransactionsListDTO getMerchantTransactionsProcessed(
            String merchantId,
            String organizationRole,
            String initiativeId,
            String fiscalCode,
            String status,
            String rewardBatchId,
            String rewardBatchTrxStatus,
            String pointOfSaleId,
            String trxCode,
            Pageable pageable) {

        String userId = StringUtils.isNotBlank(fiscalCode) ? encryptCF(fiscalCode) : null;
        Pageable sortedPageable = applyDefaultSort(pageable);
        RewardBatchTrxStatus parsedStatus = parseRewardBatchTrxStatus(rewardBatchTrxStatus);

        List<String> processedStatuses = validateAndBuildProcessedStatuses(status);

        TrxFiltersDTO filters = buildProcessedFilters(
                merchantId, initiativeId, userId, processedStatuses, rewardBatchId,
                parsedStatus, pointOfSaleId, trxCode, organizationRole
        );

        Page<Transaction> transactionPage = transactionService.getMerchantTransactionByFilter(filters, sortedPageable);

        List<MerchantTransactionDTO> merchantTransactions = transactionPage.getContent().stream()
                .map(tx -> createMerchantTransactionDTO(filters.getInitiativeId(), tx, filters.getFiscalCode(), organizationRole))
                .toList();

        return toMerchantTransactionsListDTO(merchantTransactions, transactionPage);
    }

    @Override
    public List<String> getProcessedTransactionStatuses(String organizationRole) {
        List<String> allStatuses = getAllProcessedTransactionStatuses();
        return hasAccessToAllStatuses(organizationRole) ? allStatuses : excludeToCheckStatus(allStatuses);
    }

    private List<String> validateAndBuildProcessedStatuses(String status) {
        if (StringUtils.isBlank(status)) {
            return Collections.emptyList();
        }

        String upperStatus = status.toUpperCase();
        if (TrxFiltersDTO.PROCESSED_ALLOWED_STATUSES.contains(upperStatus)) {
            return List.of(upperStatus);
        } else {
            throw new TransactionMissingParametersException(STATUS_NOT_ALLOWED,
                    STATUS_NOT_ALLOWED_MESSAGE.formatted(TrxFiltersDTO.PROCESSED_ALLOWED_STATUSES.toString()));
        }
    }

    private RewardBatchTrxStatus parseRewardBatchTrxStatus(String rewardBatchTrxStatus) {
        if (StringUtils.isBlank(rewardBatchTrxStatus)) {
            return null;
        }
        try {
            return RewardBatchTrxStatus.valueOf(rewardBatchTrxStatus);
        } catch (IllegalArgumentException _) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid rewardBatchTrxStatus value: " + rewardBatchTrxStatus);
        }
    }

    private List<String> getAllProcessedTransactionStatuses() {
        return Arrays.stream(RewardBatchTrxStatus.values())
                .map(Enum::name)
                .toList();
    }

    private List<String> excludeToCheckStatus(List<String> statuses) {
        return statuses.stream()
                .filter(status -> !TO_CHECK_STATUS.equalsIgnoreCase(status))
                .toList();
    }

    private boolean hasAccessToAllStatuses(String role) {
        return role == null || !EXCLUDED_OPERATORS.contains(role.toLowerCase(Locale.ROOT));
    }

    private MerchantTransactionDTO populateMerchantTransactionDTO(Transaction transaction) {
        String[] trxCodeUrls = resolveTrxCodeUrls(transaction.getChannel(), transaction.getTrxCode());
        Pair<Boolean, Long> splitPaymentAndResidualAmount = CommonPaymentUtilities
                .getSplitPaymentAndResidualAmountCents(transaction.getAmountCents(), transaction.getRewardCents());

        return new MerchantTransactionDTO(
                transaction.getTrxCode(),
                transaction.getCorrelationId(),
                transaction.getUserId() != null ? decryptCF(transaction.getUserId()) : null,
                transaction.getAmountCents(),
                Objects.requireNonNullElse(transaction.getRewardCents(), 0L),
                transaction.getTrxDate().toLocalDateTime(),
                CommonUtilities.minutesToSeconds(authorizationExpirationMinutes),
                transaction.getUpdateDate(),
                transaction.getStatus(),
                splitPaymentAndResidualAmount.getKey(),
                splitPaymentAndResidualAmount.getValue(),
                transaction.getChannel(),
                trxCodeUrls[0],
                trxCodeUrls[1],
                transaction.getAdditionalProperties(),
                transaction.getElaborationDateTime(),
                transaction.getPointOfSaleId(),
                transaction.getTrxDate(),
                transaction.getEffectiveAmountCents(),
                transaction.getInvoiceData(),
                null,
                null,
                null,
                transaction.getFranchiseName()
        );
    }

    private MerchantTransactionDTO createMerchantTransactionDTO(
            String initiativeId,
            Transaction transaction,
            String fiscalCode,
            String organizationRole) {

        RewardBatchTrxStatus original = Optional.ofNullable(transaction.getRewardBatchStatusTrx())
                .map(RewardBatchTrxStatus::valueOf)
                .orElse(null);

        RewardBatchTrxStatus exposed = (original == RewardBatchTrxStatus.TO_CHECK && hasAccessToAllStatuses(organizationRole))
                ? RewardBatchTrxStatus.CONSULTABLE
                : original;

        long rewardAmount = Optional.ofNullable(transaction.getRewards())
                .map(rewards -> rewards.get(initiativeId))
                .map(reward -> Objects.requireNonNullElse(reward.getAccruedRewardCents(), 0L))
                .orElse(0L);

        OffsetDateTime trxDateTime = Optional.ofNullable(transaction.getTrxDate())
                .map(OffsetDateTime::from)
                .orElse(OffsetDateTime.MIN);

        return MerchantTransactionDTO.builder()
                .trxId(transaction.getId())
                .fiscalCode(Objects.requireNonNullElse(fiscalCode, MISSING_VALUE_PLACEHOLDER))
                .effectiveAmountCents(transaction.getAmountCents())
                .rewardAmountCents(rewardAmount)
                .trxDate(trxDateTime.toLocalDateTime())
                .elaborationDateTime(transaction.getElaborationDateTime())
                .status(transaction.getStatus())
                .channel(transaction.getChannel())
                .trxChargeDate(transaction.getTrxChargeDate())
                .additionalProperties(transaction.getAdditionalProperties())
                .trxCode(transaction.getTrxCode())
                .authorizedAmountCents(transaction.getAmountCents() - rewardAmount)
                .invoiceData(Objects.requireNonNullElseGet(transaction.getInvoiceData(), InvoiceData::new))
                .rewardBatchTrxStatus(exposed)
                .pointOfSaleId(Objects.requireNonNullElse(transaction.getPointOfSaleId(), MISSING_VALUE_PLACEHOLDER))
                .franchiseName(Objects.requireNonNullElse(transaction.getFranchiseName(), MISSING_VALUE_PLACEHOLDER))
                .build();
    }

    private String decryptCF(String userId) {
        try {
            return decryptRestConnector.getPiiByToken(userId).getPii();
        } catch (Exception e) {
            throw new PDVInvocationException("An error occurred during decryption", true, e);
        }
    }

    private String encryptCF(String fiscalCode) {
        try {
            return encryptRestConnector.upsertToken(new CFDTO(fiscalCode)).getToken();
        } catch (Exception e) {
            throw new PDVInvocationException("An error occurred during encryption", true, e);
        }
    }

    private Pageable applyDefaultSort(Pageable pageable) {
        if (pageable == null) {
            return null;
        }
        if (pageable.getSort().isUnsorted()) {
            return PageRequest.of(
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    Sort.by(Sort.Direction.DESC, DEFAULT_PROCESSED_SORT_FIELD)
            );
        }
        return pageable;
    }

    private TrxFiltersDTO buildProcessedFilters(
            String merchantId,
            String initiativeId,
            String userId,
            List<String> statuses,
            String rewardBatchId,
            RewardBatchTrxStatus rewardBatchTrxStatus,
            String pointOfSaleId,
            String trxCode,
            String organizationRole) {

        TrxFiltersDTO filters = new TrxFiltersDTO();
        filters.setMerchantId(merchantId);
        filters.setInitiativeId(initiativeId);
        filters.setUserId(userId);
        filters.setStatuses(statuses);
        filters.setRewardBatchId(rewardBatchId);
        filters.setRewardBatchTrxStatus(rewardBatchTrxStatus);
        filters.setPointOfSaleId(pointOfSaleId);
        filters.setTrxCode(trxCode);
        filters.setIncludeToCheckWithConsultable(hasAccessToAllStatuses(organizationRole) && rewardBatchTrxStatus == RewardBatchTrxStatus.CONSULTABLE);
        return filters;
    }

    private String[] resolveTrxCodeUrls(String channel, String trxCode) {
        if (channel == null || RewardConstants.TRX_CHANNEL_QRCODE.equalsIgnoreCase(channel)) {
            return new String[]{
                    transactionMapper.generateTrxCodeImgUrl(trxCode),
                    transactionMapper.generateTrxCodeTxtUrl(trxCode)
            };
        }
        return new String[]{null, null};
    }

    private MerchantTransactionsListDTO toMerchantTransactionsListDTO(
            List<MerchantTransactionDTO> merchantTransactions,
            Page<?> page) {
        return new MerchantTransactionsListDTO(
                merchantTransactions,
                page.getNumber(),
                page.getSize(),
                (int) page.getTotalElements(),
                page.getTotalPages()
        );
    }
}