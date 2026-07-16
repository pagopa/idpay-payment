package it.gov.pagopa.payment.service;

import it.gov.pagopa.common.utils.CommonUtilities;
import it.gov.pagopa.payment.connector.decrypt.DecryptRestConnector;
import it.gov.pagopa.payment.connector.encrypt.EncryptRestConnector;
import it.gov.pagopa.payment.dto.*;
import it.gov.pagopa.payment.dto.mapper.TransactionInProgress2TransactionResponseMapper;
import it.gov.pagopa.payment.entity.Transaction;
import it.gov.pagopa.payment.enums.RewardBatchTrxStatus;
import it.gov.pagopa.payment.exception.custom.PDVInvocationException;
import it.gov.pagopa.payment.model.InvoiceData;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.utils.CommonPaymentUtilities;
import it.gov.pagopa.payment.utils.RewardConstants;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
public class MerchantTransactionServiceImpl implements MerchantTransactionService {
    private static final String DEFAULT_PROCESSED_SORT_FIELD = "rewardBatchStatusTrx";

    private final int authorizationExpirationMinutes;

    private final DecryptRestConnector decryptRestConnector;
    private final EncryptRestConnector encryptRestConnector;
    private final TransactionService transactionService;
    private final TransactionInProgressRepository transactionInProgressRepository;
    private final TransactionInProgress2TransactionResponseMapper transactionInProgress2TransactionResponseMapper;

    private static final Set<String> OPERATORS = Set.of("operator1", "operator2", "operator3");

    public MerchantTransactionServiceImpl(
            @Value("${app.common.expirations.authorizationMinutes}") int authorizationExpirationMinutes,

            DecryptRestConnector decryptRestConnector,
            EncryptRestConnector encryptRestConnector,
            TransactionService transactionService,
            TransactionInProgressRepository transactionInProgressRepository,
            TransactionInProgress2TransactionResponseMapper transactionInProgress2TransactionResponseMapper) {
        this.authorizationExpirationMinutes = authorizationExpirationMinutes;
        this.decryptRestConnector = decryptRestConnector;
        this.encryptRestConnector = encryptRestConnector;
        this.transactionService = transactionService;
        this.transactionInProgressRepository = transactionInProgressRepository;
        this.transactionInProgress2TransactionResponseMapper = transactionInProgress2TransactionResponseMapper;
    }

    @Override
    public MerchantTransactionsListDTO getMerchantTransactions(String merchantId, String initiativeId, String fiscalCode, String status, Pageable pageable) {
        String userId = StringUtils.isNotBlank(fiscalCode) ? encryptCF(fiscalCode) : null;
        Criteria criteria = transactionInProgressRepository.getCriteria(merchantId, null, initiativeId, userId, status, null, null);
        List<TransactionInProgress> transactionInProgressList = transactionInProgressRepository.findByFilter(criteria, pageable);
        List<MerchantTransactionDTO> merchantTransactions = transactionInProgressList.stream()
                .map(this::populateMerchantTransactionDTO)
                .toList();
        long count = transactionInProgressRepository.getCount(criteria);
        final Page<TransactionInProgress> result = PageableExecutionUtils.getPage(transactionInProgressList,
                CommonUtilities.getPageable(pageable), () -> count);
        return toMerchantTransactionsListDTO(merchantTransactions, result);
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

        pageable = applyDefaultSort(pageable);

        RewardBatchTrxStatus parsedRewardBatchTrxStatus = parseRewardBatchTrxStatus(rewardBatchTrxStatus);

        TrxFiltersDTO filters = buildProcessedFilters(
                merchantId,
                initiativeId,
                userId,
                status,
                rewardBatchId,
                parsedRewardBatchTrxStatus,
                pointOfSaleId,
                trxCode,
                organizationRole
        );

        Page<Transaction> transactionPage = transactionService.getMerchantTransactionByFilter(filters, pageable);

        List<MerchantTransactionDTO> merchantTransactions = transactionPage.getContent().stream()
                .map(transaction -> createMerchantTransactionDTO(filters.getInitiativeId(), transaction, filters.getFiscalCode(), organizationRole))
                .toList();

        return toMerchantTransactionsListDTO(merchantTransactions, transactionPage);
    }

    private RewardBatchTrxStatus parseRewardBatchTrxStatus(String rewardBatchTrxStatus) {
        if (StringUtils.isBlank(rewardBatchTrxStatus)) {
            return null;
        }
        try {
            return RewardBatchTrxStatus.valueOf(rewardBatchTrxStatus);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid rewardBatchTrxStatus value: " + rewardBatchTrxStatus);
        }
    }

    private boolean isOperator(String role) {
        return role == null || !OPERATORS.contains(role.toLowerCase());
    }

    private MerchantTransactionDTO populateMerchantTransactionDTO(TransactionInProgress transaction){
        String[] trxCodeUrls = resolveTrxCodeUrls(transaction.getChannel(), transaction.getTrxCode());
        Pair<Boolean, Long> splitPaymentAndResidualAmountCents = CommonPaymentUtilities.getSplitPaymentAndResidualAmountCents(transaction.getAmountCents(), transaction.getRewardCents());

        return new MerchantTransactionDTO(transaction.getTrxCode(),
                transaction.getCorrelationId(),
                transaction.getUserId() != null ? decryptCF(transaction.getUserId()) : null,
                transaction.getAmountCents(),
                transaction.getRewardCents() != null ? transaction.getRewardCents() : Long.valueOf(0),
                transaction.getTrxDate().toLocalDateTime(),
                CommonUtilities.minutesToSeconds(authorizationExpirationMinutes),
                transaction.getUpdateDate(),
                transaction.getStatus(),
                splitPaymentAndResidualAmountCents.getKey(),
                splitPaymentAndResidualAmountCents.getValue(),
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


        RewardBatchTrxStatus original = transaction.getRewardBatchStatusTrx() != null ? RewardBatchTrxStatus.valueOf(transaction.getRewardBatchStatusTrx()) : null;
        RewardBatchTrxStatus exposed = original;

        if (isOperator(organizationRole) && original == RewardBatchTrxStatus.TO_CHECK) {
            exposed = RewardBatchTrxStatus.CONSULTABLE;
        }

        return MerchantTransactionDTO.builder()
                .trxId(transaction.getId())
                .fiscalCode(fiscalCode != null ? fiscalCode : "-")
                .effectiveAmountCents(transaction.getAmountCents())
                .rewardAmountCents(transaction.getRewards().get(initiativeId).getAccruedRewardCents())
                .trxDate(transaction.getTrxDate() == null ? LocalDateTime.MIN : transaction.getTrxDate().toLocalDateTime())
                .elaborationDateTime(transaction.getElaborationDateTime())
                .status(transaction.getStatus())
                .channel(transaction.getChannel())
                .trxChargeDate(transaction.getTrxChargeDate())
                .additionalProperties(transaction.getAdditionalProperties())
                .trxCode(transaction.getTrxCode())
                .authorizedAmountCents(transaction.getAmountCents()
                        - transaction.getRewards().get(initiativeId).getAccruedRewardCents())
                .invoiceData(transaction.getInvoiceData() != null ? transaction.getInvoiceData() : new InvoiceData())
                .rewardBatchTrxStatus(exposed)
                .pointOfSaleId(transaction.getPointOfSaleId() == null ? "-" : transaction.getPointOfSaleId())
                //.rewardBatchRejectionReason(sortedReasons(transaction.getRewardBatchRejectionReason()))
                //.checksError(checksErrorMapper.toDto(transaction.getChecksError()))
                .franchiseName(transaction.getFranchiseName() == null ? "-" : transaction.getFranchiseName())
                .build();
    }

    private String decryptCF(String userId) {
        String fiscalCode;
        try {
            DecryptCfDTO decryptedCfDTO = decryptRestConnector.getPiiByToken(userId);
            fiscalCode = decryptedCfDTO.getPii();
        } catch (Exception e) {
            throw new PDVInvocationException("An error occurred during decryption",true,e);
        }
        return fiscalCode;
    }

    private String encryptCF(String fiscalCode) {
        String userId;
        try {
            EncryptedCfDTO encryptedCfDTO = encryptRestConnector.upsertToken(new CFDTO(fiscalCode));
            userId = encryptedCfDTO.getToken();
        } catch (Exception e) {
            throw new PDVInvocationException("An error occurred during encryption",true,e);
        }
        return userId;
    }

    private Pageable applyDefaultSort(Pageable pageable) {
        if (pageable.getSort().isUnsorted()) {
            return PageRequest.of(
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    Sort.by(Sort.Direction.DESC, DEFAULT_PROCESSED_SORT_FIELD)
            );
        }
        return pageable;
    }

    private TrxFiltersDTO buildProcessedFilters(String merchantId,
                                                String initiativeId,
                                                String userId,
                                                String status,
                                                String rewardBatchId,
                                                RewardBatchTrxStatus rewardBatchTrxStatus,
                                                String pointOfSaleId,
                                                String trxCode,
                                                String organizationRole) {
        TrxFiltersDTO filters = new TrxFiltersDTO();
        filters.setMerchantId(merchantId);
        filters.setInitiativeId(initiativeId);
        filters.setUserId(userId);
        filters.setStatus(status);
        filters.setRewardBatchId(rewardBatchId);
        filters.setRewardBatchTrxStatus(rewardBatchTrxStatus);
        filters.setPointOfSaleId(pointOfSaleId);
        filters.setTrxCode(trxCode);
        filters.setIncludeToCheckWithConsultable(isOperator(organizationRole) && rewardBatchTrxStatus == RewardBatchTrxStatus.CONSULTABLE);
        return filters;
    }

    private String[] resolveTrxCodeUrls(String channel, String trxCode) {
        if (channel == null || RewardConstants.TRX_CHANNEL_QRCODE.equalsIgnoreCase(channel)) {
            return new String[]{
                    transactionInProgress2TransactionResponseMapper.generateTrxCodeImgUrl(trxCode),
                    transactionInProgress2TransactionResponseMapper.generateTrxCodeTxtUrl(trxCode)
            };
        }
        return new String[]{null, null};
    }

    private MerchantTransactionsListDTO toMerchantTransactionsListDTO(List<MerchantTransactionDTO> merchantTransactions,
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
