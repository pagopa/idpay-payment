package it.gov.pagopa.payment.repository;

import com.mongodb.client.result.UpdateResult;
import it.gov.pagopa.payment.exception.ClientExceptionNoBody;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.model.TransactionInProgress.Fields;
import it.gov.pagopa.payment.utils.Utils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

public class TransactionInProgressRepositoryExtImpl implements TransactionInProgressRepositoryExt {

    private final int trxThrottlingSeconds;

    private final MongoTemplate mongoTemplate;

    public TransactionInProgressRepositoryExtImpl(
            @Value("${app.qrCode.throttlingSeconds}") int trxThrottlingSeconds,
            MongoTemplate mongoTemplate
    ) {
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
                        .setOnInsert(Fields.acquirerCode, trx.getAcquirerCode())
                        .setOnInsert(Fields.amountCents, trx.getAmountCents())
                        .setOnInsert(Fields.effectiveAmount, Utils.centsToEuro(trx.getAmountCents()))
                        .setOnInsert(Fields.amountCurrency, trx.getAmountCurrency())
                        .setOnInsert(Fields.merchantFiscalCode, trx.getMerchantFiscalCode())
                        .setOnInsert(Fields.merchantId, trx.getMerchantId())
                        .setOnInsert(Fields.callbackUrl, trx.getCallbackUrl())
                        .setOnInsert(Fields.idTrxAcquirer, trx.getIdTrxAcquirer())
                        .setOnInsert(Fields.idTrxIssuer, trx.getIdTrxIssuer())
                        .setOnInsert(Fields.initiativeId, trx.getInitiativeId())
                        .setOnInsert(Fields.mcc, trx.getMcc())
                        .setOnInsert(Fields.senderCode, trx.getSenderCode())
                        .setOnInsert(Fields.vat, trx.getVat())
                        .setOnInsert(Fields.trxDate, trx.getTrxDate())
                        .setOnInsert(Fields.trxChargeDate, trx.getTrxChargeDate())
                        .setOnInsert(Fields.status, trx.getStatus())
                        .setOnInsert(Fields.operationType, trx.getOperationType())
                        .setOnInsert(Fields.operationTypeTranscoded, trx.getOperationTypeTranscoded())
                        .setOnInsert(Fields.trxCode, trxCode),
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
                        .set(Fields.elaborationDateTime, LocalDateTime.now()),
                TransactionInProgress.class);
        if (trx == null && mongoTemplate.exists(Query.query(criteriaById(trxId)), TransactionInProgress.class)) {
            throw new ClientExceptionNoBody(HttpStatus.TOO_MANY_REQUESTS, "Too many requests on trx having id: " + trxId);
        }

        return trx;
    }

    private Criteria criteriaById(String trxId) {
        return Criteria.where(Fields.id).is(trxId);
    }
}
