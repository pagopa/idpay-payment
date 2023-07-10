package it.gov.pagopa.payment.repository;

import com.mongodb.client.result.UpdateResult;
import it.gov.pagopa.common.utils.CommonUtilities;
import it.gov.pagopa.common.web.exception.ClientExceptionNoBody;
import it.gov.pagopa.payment.dto.Reward;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.model.TransactionInProgress.Fields;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class TransactionInProgressRepositoryExtImpl implements TransactionInProgressRepositoryExt {

    private final MongoTemplate mongoTemplate;
    private final long trxThrottlingSeconds;
    private final long authorizationExpirationMinutes;
    private final long cancelExpirationMinutes;

    public TransactionInProgressRepositoryExtImpl(
            MongoTemplate mongoTemplate,
            @Value("${app.qrCode.throttlingSeconds:1}") long trxThrottlingSeconds,
            @Value("${app.qrCode.expirations.authorizationMinutes:15}") long authorizationExpirationMinutes,
            @Value("${app.qrCode.expirations.cancelMinutes:15}") long cancelExpirationMinutes) {
        this.mongoTemplate = mongoTemplate;
        this.trxThrottlingSeconds = trxThrottlingSeconds;
        this.authorizationExpirationMinutes = authorizationExpirationMinutes;
        this.cancelExpirationMinutes = cancelExpirationMinutes;
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
                        .setOnInsert(Fields.effectiveAmount, CommonUtilities.centsToEuro(trx.getAmountCents()))
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
                        .setOnInsert(Fields.updateDate, trx.getUpdateDate()),
                TransactionInProgress.class);
    }

    @Override
    public TransactionInProgress findByTrxCodeAndAuthorizationNotExpired(String trxCode) {
        return mongoTemplate.findOne(
                Query.query(
                        criteriaByTrxCodeAndChargeDateGreaterThan(trxCode, LocalDateTime.now().minusMinutes(authorizationExpirationMinutes))),
                TransactionInProgress.class);
    }

    @Override
    public TransactionInProgress findByTrxCodeAndAuthorizationNotExpiredThrottled(String trxCode) {
        LocalDateTime minTrxChargeDate = LocalDateTime.now().minusMinutes(authorizationExpirationMinutes);
        TransactionInProgress transaction =
                mongoTemplate.findAndModify(
                        Query.query(
                                criteriaByTrxCodeAndChargeDateGreaterThan(trxCode, minTrxChargeDate)
                                        .andOperator(criteriaByAuthDateThrottled())),
                        new Update()
                                .currentDate(Fields.authDate)
                                .currentDate(Fields.updateDate),
                        FindAndModifyOptions.options().returnNew(true),
                        TransactionInProgress.class);

        if (transaction == null
                && mongoTemplate.exists(
                Query.query(criteriaByTrxCodeAndChargeDateGreaterThan(trxCode, minTrxChargeDate)),
                TransactionInProgress.class)) {
            throw new ClientExceptionNoBody(
                    HttpStatus.TOO_MANY_REQUESTS, "Too many requests on trx having trCode: " + trxCode);
        }

        return transaction;
    }

    private Criteria criteriaByTrxCodeAndChargeDateGreaterThan(String trxCode, LocalDateTime trxChargeDate) {
        return Criteria.where(Fields.trxCode).is(trxCode).and(Fields.trxChargeDate).gte(trxChargeDate);
    }

    private Criteria criteriaByAuthDateThrottled() {
        return new Criteria()
                .orOperator(
                        Criteria.where(Fields.authDate).is(null),
                        Criteria.where(Fields.authDate)
                                .lt(LocalDateTime.now().minusSeconds(trxThrottlingSeconds)));
    }

    @Override
    public void updateTrxRejected(String id, String userId, List<String> rejectionReasons) {
        mongoTemplate.updateFirst(
                Query.query(Criteria.where(Fields.id).is(id)),
                new Update()
                        .set(Fields.status, SyncTrxStatus.REJECTED)
                        .set(Fields.userId, userId)
                        .set(Fields.reward, 0L)
                        .set(Fields.rewards, Collections.emptyMap())
                        .set(Fields.rejectionReasons, rejectionReasons)
                        .currentDate(Fields.updateDate),
                TransactionInProgress.class);
    }

    @Override
    public void updateTrxIdentified(String id, String userId, Long reward, List<String> rejectionReasons, Map<String, Reward> rewards) {
        mongoTemplate.updateFirst(
                Query.query(Criteria.where(Fields.id).is(id)),
                new Update()
                        .set(Fields.status, SyncTrxStatus.IDENTIFIED)
                        .set(Fields.userId, userId)
                        .set(Fields.reward, reward)
                        .set(Fields.rejectionReasons, rejectionReasons)
                        .set(Fields.rewards, rewards)
                        .currentDate(Fields.updateDate),
                TransactionInProgress.class);
    }

    @Override
    public void updateTrxAuthorized(TransactionInProgress trx, Long reward, List<String> rejectionReasons) {
        mongoTemplate.updateFirst(
                Query.query(Criteria.where(Fields.id).is(trx.getId())),
                new Update()
                        .set(Fields.status, SyncTrxStatus.AUTHORIZED)
                        .set(Fields.reward, reward)
                        .set(Fields.rejectionReasons, rejectionReasons)
                        .set(Fields.rewards, trx.getRewards())
                        .currentDate(Fields.updateDate),
                TransactionInProgress.class);
    }

    @Override
    public void updateTrxRejected(String id, List<String> rejectionReasons) {
        mongoTemplate.updateFirst(
                Query.query(Criteria.where(Fields.id).is(id)),
                new Update()
                        .set(Fields.status, SyncTrxStatus.REJECTED)
                        .set(Fields.reward, 0L)
                        .set(Fields.rewards, Collections.emptyMap())
                        .set(Fields.rejectionReasons, rejectionReasons)
                        .currentDate(Fields.updateDate),
                TransactionInProgress.class);
    }

    @Override
    public TransactionInProgress findByIdThrottled(String trxId) {
        TransactionInProgress trx = mongoTemplate.findAndModify(
                Query.query(criteriaById(trxId)
                        .orOperator(
                                Criteria.where(Fields.elaborationDateTime).is(null),
                                Criteria.where(Fields.elaborationDateTime).lt(LocalDateTime.now().minusSeconds(trxThrottlingSeconds)))
                ),
                new Update()
                        .currentDate(Fields.elaborationDateTime)
                        .currentDate(Fields.updateDate),
                FindAndModifyOptions.options().returnNew(true),
                TransactionInProgress.class);
        if (trx == null && mongoTemplate.exists(Query.query(criteriaById(trxId)), TransactionInProgress.class)) {
            throw new ClientExceptionNoBody(HttpStatus.TOO_MANY_REQUESTS, "Too many requests on trx having id: " + trxId);
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
            if (List.of(SyncTrxStatus.CREATED.toString(), SyncTrxStatus.IDENTIFIED.toString(), SyncTrxStatus.REJECTED.toString())
                    .contains(status)) {criteria.orOperator(Criteria.where(Fields.status).is(SyncTrxStatus.CREATED),
                    Criteria.where(Fields.status).is(SyncTrxStatus.IDENTIFIED),
                    Criteria.where(Fields.status).is(SyncTrxStatus.REJECTED));
            } else {
                criteria.and(TransactionInProgress.Fields.status).is(status);
            }
        }
        return criteria;
    }

    @Override
    public List<TransactionInProgress> findByFilter(Criteria criteria, Pageable pageable){
        return mongoTemplate.find(Query.query(criteria).with(CommonUtilities.getPageable(pageable)), TransactionInProgress.class);}

    @Override
    public long getCount(Criteria criteria) {
        return mongoTemplate.count(Query.query(criteria), TransactionInProgress.class);
    }

    @Override
    public TransactionInProgress findCancelExpiredTransaction() {
        return findExpiredTransaction(cancelExpirationMinutes, List.of(SyncTrxStatus.AUTHORIZED));
    }

    @Override
    public TransactionInProgress findAuthorizationExpiredTransaction() {
        return findExpiredTransaction(authorizationExpirationMinutes, List.of(SyncTrxStatus.IDENTIFIED, SyncTrxStatus.CREATED, SyncTrxStatus.REJECTED));
    }

    private TransactionInProgress findExpiredTransaction(long expirationMinutes, List<SyncTrxStatus> statusList) {
        LocalDateTime now = LocalDateTime.now();

        Query query = Query.query(
                Criteria.where(Fields.trxChargeDate).lt(now.minusMinutes(expirationMinutes)).andOperator(
                        Criteria.where(Fields.status).in(statusList)
                                .orOperator(
                                        Criteria.where(Fields.elaborationDateTime).is(null),
                                        Criteria.where(Fields.elaborationDateTime).lt(now.minusSeconds(trxThrottlingSeconds))
                                )
                )
        );

        return mongoTemplate.findAndModify(query,
                new Update()
                        .currentDate(Fields.elaborationDateTime),
                FindAndModifyOptions.options().returnNew(true),
                TransactionInProgress.class);
    }
}
