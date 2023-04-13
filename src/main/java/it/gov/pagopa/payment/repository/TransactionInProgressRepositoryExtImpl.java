package it.gov.pagopa.payment.repository;

import com.mongodb.client.result.UpdateResult;
import it.gov.pagopa.payment.dto.Reward;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.exception.ClientExceptionNoBody;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.model.TransactionInProgress.Fields;
import it.gov.pagopa.payment.utils.Utils;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;

public class TransactionInProgressRepositoryExtImpl implements TransactionInProgressRepositoryExt {

  private final MongoTemplate mongoTemplate;
  private final long trxThrottlingSeconds;
  private final long trxInProgressLifetimeMinutes;

  public TransactionInProgressRepositoryExtImpl(
      MongoTemplate mongoTemplate,
      @Value("${app.qrCode.throttlingSeconds}") long trxThrottlingSeconds,
      @Value("${app.qrCode.trxInProgressLifetimeMinutes}") long trxInProgressLifetimeMinutes) {
    this.mongoTemplate = mongoTemplate;
    this.trxThrottlingSeconds = trxThrottlingSeconds;
    this.trxInProgressLifetimeMinutes = trxInProgressLifetimeMinutes;
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
  public TransactionInProgress findByTrxCodeThrottled(String trxCode) {
    LocalDateTime trxChargeDate = LocalDateTime.now().minusMinutes(trxInProgressLifetimeMinutes);
    TransactionInProgress transaction =
        mongoTemplate.findAndModify(
            Query.query(
                criteriaByTrxCodeAndChargeDate(trxCode, trxChargeDate)
                    .andOperator(criteriaByAuthDate())),
            new Update().set(Fields.authDate, LocalDateTime.now()),
            FindAndModifyOptions.options().returnNew(true),
            TransactionInProgress.class);
    if (transaction == null
        && mongoTemplate.exists(
            Query.query(criteriaByTrxCodeAndChargeDate(trxCode, trxChargeDate)),
            TransactionInProgress.class)) {
      throw new ClientExceptionNoBody(
          HttpStatus.TOO_MANY_REQUESTS, "Too many requests on trx having trCode: " + trxCode);
    }
    return transaction;
  }

  @Override
  public void updateTrxAuthorized(String id, Reward reward, List<String> rejectionReasons) {
    mongoTemplate.updateFirst(
        Query.query(Criteria.where(Fields.id).is(id)),
        new Update()
            .set(Fields.status, SyncTrxStatus.AUTHORIZED)
            .set(Fields.reward, reward)
            .set(Fields.rejectionReasons, rejectionReasons),
        TransactionInProgress.class);
  }

  @Override
  public TransactionInProgress findByTrxCode(String trxCode) {
    return mongoTemplate.findOne(
        Query.query(
            criteriaByTrxCodeAndChargeDate(trxCode, LocalDateTime.now().minusMinutes(trxInProgressLifetimeMinutes))),
            TransactionInProgress.class);
  }

  @Override
  public void updateTrxRejected(String id, String userId, List<String> rejectionReasons) {
    mongoTemplate.updateFirst(
        Query.query(Criteria.where(Fields.id).is(id)),
        new Update()
            .set(Fields.status, SyncTrxStatus.REJECTED)
            .set(Fields.userId, userId)
            .set(Fields.rejectionReasons, rejectionReasons),
        TransactionInProgress.class);
  }

  @Override
  public void updateTrxIdentified(String id, String userId) {
    mongoTemplate.updateFirst(
        Query.query(Criteria.where(Fields.id).is(id)),
        new Update().set(Fields.status, SyncTrxStatus.IDENTIFIED).set(Fields.userId, userId),
        TransactionInProgress.class);
  }

  private Criteria criteriaByTrxCodeAndChargeDate(String trxCode, LocalDateTime trxChargeDate) {
    return Criteria.where(Fields.trxCode).is(trxCode).and(Fields.trxChargeDate).gte(trxChargeDate);
  }

  private Criteria criteriaByAuthDate() {
    return new Criteria()
        .orOperator(
            Criteria.where(Fields.authDate).is(null),
            Criteria.where(Fields.authDate)
                .lt(LocalDateTime.now().minusSeconds(trxThrottlingSeconds)));
    }
}
