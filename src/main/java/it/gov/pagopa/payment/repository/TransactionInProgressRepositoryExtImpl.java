package it.gov.pagopa.payment.repository;

import com.mongodb.client.result.UpdateResult;
import it.gov.pagopa.common.mongo.utils.MongoConstants;
import it.gov.pagopa.common.utils.CommonUtilities;
import it.gov.pagopa.common.web.exception.ClientExceptionNoBody;
import it.gov.pagopa.payment.exception.custom.toomanyrequests.TooManyRequestsException;
import it.gov.pagopa.payment.constants.PaymentConstants;
import it.gov.pagopa.payment.constants.PaymentConstants.ExceptionCode;
import it.gov.pagopa.payment.dto.Reward;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.model.TransactionInProgress.Fields;
import java.time.OffsetDateTime;

import it.gov.pagopa.payment.utils.RewardConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.ArithmeticOperators;
import org.springframework.data.mongodb.core.aggregation.ComparisonOperators;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
public class TransactionInProgressRepositoryExtImpl implements TransactionInProgressRepositoryExt {

    private final MongoTemplate mongoTemplate;
    private final long trxThrottlingSeconds;

    public TransactionInProgressRepositoryExtImpl(
            MongoTemplate mongoTemplate,
            @Value("${app.qrCode.throttlingSeconds:1}") long trxThrottlingSeconds) {
        this.mongoTemplate = mongoTemplate;
        this.trxThrottlingSeconds = trxThrottlingSeconds;
    }

    @Override
    public UpdateResult createIfExists(TransactionInProgress trx, String trxCode) {
        trx.setTrxCode(trxCode);
        return mongoTemplate.upsert(
                Query.query(Criteria.where(Fields.trxCode).is(trx.getTrxCode())),
                new Update()
                        .setOnInsert(Fields.id, trx.getId())
                        .setOnInsert(Fields.correlationId, trx.getCorrelationId())
                        .setOnInsert(Fields.acquirerId, trx.getAcquirerId())
                        .setOnInsert(Fields.amountCents, trx.getAmountCents())
                        .setOnInsert(Fields.effectiveAmount, trx.getAmountCents() != null ? CommonUtilities.centsToEuro(trx.getAmountCents()) : null)
                        .setOnInsert(Fields.amountCurrency, trx.getAmountCurrency())
                        .setOnInsert(Fields.merchantFiscalCode, trx.getMerchantFiscalCode())
                        .setOnInsert(Fields.merchantId, trx.getMerchantId())
                        .setOnInsert(Fields.idTrxAcquirer, trx.getIdTrxAcquirer())
                        .setOnInsert(Fields.idTrxIssuer, trx.getIdTrxIssuer())
                        .setOnInsert(Fields.initiativeId, trx.getInitiativeId())
                        .setOnInsert(Fields.mcc, trx.getMcc())
                        .setOnInsert(Fields.vat, trx.getVat())
                        .setOnInsert(Fields.trxDate, trx.getTrxDate())
                        .setOnInsert(Fields.trxChargeDate, trx.getTrxChargeDate())
                        .setOnInsert(Fields.status, trx.getStatus())
                        .setOnInsert(Fields.operationType, trx.getOperationType())
                        .setOnInsert(Fields.operationTypeTranscoded, trx.getOperationTypeTranscoded())
                        .setOnInsert(Fields.channel, trx.getChannel())
                        .setOnInsert(Fields.trxCode, trxCode)
                        .setOnInsert(Fields.initiativeName, trx.getInitiativeName())
                        .setOnInsert(Fields.businessName, trx.getBusinessName())
                        .setOnInsert(Fields.updateDate, trx.getUpdateDate())
                        .setOnInsert(Fields.userId, trx.getUserId()),
                TransactionInProgress.class);
    }

    @Override
    public TransactionInProgress findByTrxCodeAndAuthorizationNotExpired(String trxCode, long authorizationExpirationMinutes) {
        return mongoTemplate.findOne(
                Query.query(
                        criteriaByTrxCodeAndDateGreaterThan(trxCode, LocalDateTime.now().minusMinutes(authorizationExpirationMinutes))),
                TransactionInProgress.class);
    }

    @Override
    public TransactionInProgress findByTrxIdAndAuthorizationNotExpired(String trxId, long authorizationExpirationMinutes) {
        return mongoTemplate.findOne(
                Query.query(
                        criteriaByTrxIdAndDateGreaterThan(trxId, LocalDateTime.now().minusMinutes(authorizationExpirationMinutes))),
                        TransactionInProgress.class);
    }

    @Override
    public TransactionInProgress findByTrxCodeAndAuthorizationNotExpiredThrottled(String trxCode, long authorizationExpirationMinutes) {
        LocalDateTime minTrxDate = LocalDateTime.now().minusMinutes(authorizationExpirationMinutes);
        TransactionInProgress transaction =
                mongoTemplate.findAndModify(
                        Query.query(
                                criteriaByTrxCodeAndDateGreaterThan(trxCode, minTrxDate)
                                        .andOperator(criteriaByTrxChargeDateThrottled())),
                        new Update()
                                .currentDate(Fields.trxChargeDate)
                                .currentDate(Fields.updateDate),
                        FindAndModifyOptions.options().returnNew(true),
                        TransactionInProgress.class);

        if (transaction == null
                && mongoTemplate.exists(
                Query.query(criteriaByTrxCodeAndDateGreaterThan(trxCode, minTrxDate)),
                TransactionInProgress.class)) {
            throw new ClientExceptionNoBody(
                    HttpStatus.TOO_MANY_REQUESTS, "Too many requests on trx having trCode: " + trxCode);
        }

        return transaction;
    }

    private Criteria criteriaByTrxCodeAndDateGreaterThan(String trxCode, LocalDateTime trxDate) {
        return Criteria.where(Fields.trxCode).is(trxCode).and(Fields.trxDate).gte(trxDate);
    }
    private Criteria criteriaByTrxIdAndDateGreaterThan(String trxId, LocalDateTime trxDate) {
        return Criteria.where(Fields.id).is(trxId).and(Fields.trxDate).gte(trxDate);
    }

    private Criteria criteriaByTrxChargeDateThrottled() {
        return new Criteria()
                .orOperator(
                        Criteria.where(Fields.trxChargeDate).is(null),
                        Criteria.expr(
                                ComparisonOperators.Lt.valueOf(Fields.trxChargeDate)
                                        .lessThan(ArithmeticOperators.Subtract.valueOf(MongoConstants.AGGREGATION_EXPRESSION_VARIABLE_NOW).subtract(1000*trxThrottlingSeconds))));
    }

    @Override
    public void updateTrxRejected(String id, String userId, List<String> rejectionReasons, String channel) {
        mongoTemplate.updateFirst(
                Query.query(Criteria.where(Fields.id).is(id)),
                new Update()
                        .set(Fields.status, SyncTrxStatus.REJECTED)
                        .set(Fields.userId, userId)
                        .set(Fields.reward, 0L)
                        .set(Fields.rewards, Collections.emptyMap())
                        .set(Fields.rejectionReasons, rejectionReasons)
                        .set(Fields.channel, channel)
                        .currentDate(Fields.updateDate),
                TransactionInProgress.class);
    }

    @Override
    public void updateTrxRelateUserIdentified(String id, String userId, String channel) {
        mongoTemplate.updateFirst(
                Query.query(Criteria.where(Fields.id).is(id)),
                new Update()
                        .set(Fields.userId, userId)
                        .set(Fields.status, SyncTrxStatus.IDENTIFIED)
                        .set(Fields.channel, channel)
                        .currentDate(Fields.updateDate),
                TransactionInProgress.class);
    }
    @Override
    public void updateTrxIdentified(String id, String userId, Long reward, List<String> rejectionReasons, Map<String, Reward> rewards, String channel) {
        mongoTemplate.updateFirst(
                Query.query(Criteria.where(Fields.id).is(id)),
                new Update()
                        .set(Fields.status, SyncTrxStatus.IDENTIFIED)
                        .set(Fields.userId, userId)
                        .set(Fields.reward, reward)
                        .set(Fields.rejectionReasons, rejectionReasons)
                        .set(Fields.rewards, rewards)
                        .set(Fields.channel, channel)
                        .currentDate(Fields.updateDate),
                TransactionInProgress.class);
    }

    @Override
    public void updateTrxAuthorized(TransactionInProgress trx, Long reward, List<String> rejectionReasons) {
        Update update = new Update()
                .set(Fields.status, SyncTrxStatus.AUTHORIZED)
                .set(Fields.reward, reward)
                .set(Fields.rejectionReasons, rejectionReasons)
                .set(Fields.rewards, trx.getRewards())
                .set(Fields.trxChargeDate, trx.getTrxChargeDate())
                .currentDate(Fields.updateDate);

        if(RewardConstants.TRX_CHANNEL_BARCODE.equals(trx.getChannel())){
            update.set(Fields.amountCurrency, PaymentConstants.CURRENCY_EUR)
                    .set(Fields.merchantId, trx.getMerchantId());
        }

        mongoTemplate.updateFirst(
                Query.query(Criteria.where(Fields.id).is(trx.getId())),
                update,
                TransactionInProgress.class);
    }

    @Override
    public void updateTrxRejected(String id, List<String> rejectionReasons, OffsetDateTime trxChargeDate) {
        mongoTemplate.updateFirst(
                Query.query(Criteria.where(Fields.id).is(id)),
                new Update()
                        .set(Fields.status, SyncTrxStatus.REJECTED)
                        .set(Fields.reward, 0L)
                        .set(Fields.rewards, Collections.emptyMap())
                        .set(Fields.rejectionReasons, rejectionReasons)
                        .set(Fields.trxChargeDate, trxChargeDate)
                        .currentDate(Fields.updateDate),
                TransactionInProgress.class);
    }

    @Override
    public TransactionInProgress findByIdThrottled(String trxId) {
        TransactionInProgress trx = mongoTemplate.findAndModify(
                Query.query(criteriaById(trxId)
                        .orOperator(
                                Criteria.where(Fields.elaborationDateTime).is(null),
                                Criteria.expr(
                                        ComparisonOperators.Lt.valueOf(Fields.elaborationDateTime)
                                                .lessThan(ArithmeticOperators.Subtract.valueOf(MongoConstants.AGGREGATION_EXPRESSION_VARIABLE_NOW).subtract(1000 * trxThrottlingSeconds))))
                ),
                new Update()
                        .currentDate(Fields.elaborationDateTime)
                        .currentDate(Fields.updateDate),
                FindAndModifyOptions.options().returnNew(true),
                TransactionInProgress.class);
        if (trx == null && mongoTemplate.exists(Query.query(criteriaById(trxId)), TransactionInProgress.class)) {
            throw new TooManyRequestsException(
                ExceptionCode.TOO_MANY_REQUESTS, "Too many requests on trx having id: " + trxId);
        }

        return trx;
    }

    private Criteria criteriaById(String trxId) {
        return Criteria.where(Fields.id).is(trxId);
    }

    @Override
    public Criteria getCriteria(String merchantId, String initiativeId, String userId, String status) {
        Criteria criteria = Criteria.where(Fields.merchantId).is(merchantId).and(Fields.initiativeId).is(initiativeId);
        if (userId != null) {
            criteria.and(Fields.userId).is(userId);
        }
        if (status != null) {
            if (List.of(SyncTrxStatus.CREATED.toString(), SyncTrxStatus.IDENTIFIED.toString())
                    .contains(status)) {
                criteria.orOperator(Criteria.where(Fields.status).is(SyncTrxStatus.CREATED),
                        Criteria.where(Fields.status).is(SyncTrxStatus.IDENTIFIED));
            } else {
                criteria.and(TransactionInProgress.Fields.status).is(status);
            }
        }
        return criteria;
    }

    @Override
    public List<TransactionInProgress> findByFilter(Criteria criteria, Pageable pageable) {
        return mongoTemplate.find(Query.query(criteria).with(CommonUtilities.getPageable(pageable)), TransactionInProgress.class);
    }

    @Override
    public long getCount(Criteria criteria) {
        return mongoTemplate.count(Query.query(criteria), TransactionInProgress.class);
    }

    @Override
    public TransactionInProgress findCancelExpiredTransaction(String initiativeId, long cancelExpirationMinutes) {
        return findExpiredTransaction(initiativeId, cancelExpirationMinutes, List.of(SyncTrxStatus.AUTHORIZED));
    }

    @Override
    public TransactionInProgress findAuthorizationExpiredTransaction(String initiativeId, long authorizationExpirationMinutes) {
        return findExpiredTransaction(initiativeId, authorizationExpirationMinutes, List.of(SyncTrxStatus.IDENTIFIED, SyncTrxStatus.CREATED, SyncTrxStatus.REJECTED));
    }

    @Override
    public List<TransactionInProgress> deletePaged(String initiativeId, int pageSize) {
        log.trace("[DELETE_PAGED] Deleting transactions in progress in pages");
        Pageable pageable = PageRequest.of(0, pageSize);
        return mongoTemplate.findAllAndRemove(
                Query.query(Criteria.where(Fields.initiativeId).is(initiativeId)).with(pageable),
                TransactionInProgress.class
        );
    }

    private TransactionInProgress findExpiredTransaction(String initiativeId, long expirationMinutes, List<SyncTrxStatus> statusList) {
        LocalDateTime now = LocalDateTime.now();

        Criteria criteria = Criteria.where(Fields.trxDate).lt(now.minusMinutes(expirationMinutes)).andOperator(
                Criteria.where(Fields.status).in(statusList)
                        .orOperator(
                                Criteria.where(Fields.elaborationDateTime).is(null),
                                Criteria.expr(
                                        ComparisonOperators.Lt.valueOf(Fields.elaborationDateTime)
                                                .lessThan(ArithmeticOperators.Subtract.valueOf(MongoConstants.AGGREGATION_EXPRESSION_VARIABLE_NOW).subtract(1000 * trxThrottlingSeconds))))
        );

        if (initiativeId != null) {
            criteria.and(Fields.initiativeId).is(initiativeId);
        }

        return mongoTemplate.findAndModify(Query.query(criteria),
                new Update()
                        .currentDate(Fields.elaborationDateTime),
                FindAndModifyOptions.options().returnNew(true),
                TransactionInProgress.class);
    }
}
