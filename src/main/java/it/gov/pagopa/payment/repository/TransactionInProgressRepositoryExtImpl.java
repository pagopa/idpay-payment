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
import org.springframework.data.mongodb.core.query.CriteriaDefinition;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;

@Repository
public class TransactionInProgressRepositoryExtImpl implements TransactionInProgressRepositoryExt {

  private final MongoTemplate mongoTemplate;
  private final long trxThrottlingSeconds;
  private final long trxThrottlingMinutes;

  public TransactionInProgressRepositoryExtImpl(
      MongoTemplate mongoTemplate,
      @Value("${app.qrCode.throttlingSeconds}") long trxThrottlingSeconds,
      @Value("${app.qrCode.throttlingMinutes}") long trxThrottlingMinutes) {
    this.mongoTemplate = mongoTemplate;
    this.trxThrottlingSeconds = trxThrottlingSeconds;
    this.trxThrottlingMinutes = trxThrottlingMinutes;
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
    return mongoTemplate.findAndModify(
        Query.query(
            Criteria.where(Fields.trxCode)
                .is(trxCode)
                .andOperator(
                    Criteria.where(Fields.authDate)
                        .exists(true)
                        .orOperator(
                            Criteria.where(Fields.authDate)
                                .lte(LocalDateTime.now().minusSeconds(trxThrottlingSeconds)),
                            Criteria.where(Fields.authDate).is(null)),
                    Criteria.where(Fields.trxChargeDate)
                        .gte(LocalDateTime.now().minusMinutes(trxThrottlingMinutes)))),
        new Update().set(Fields.authDate, LocalDateTime.now()),
        FindAndModifyOptions.options().returnNew(true),
        TransactionInProgress.class);
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
  public TransactionInProgress findByIdAndUserId(String id, String userId) {
    return mongoTemplate.findOne(
        Query.query(Criteria.where(Fields.id).is(id).and(Fields.userId).is(userId)),
        TransactionInProgress.class);
  }

  @Override
  public TransactionInProgress findByTrxCodeAndRelateUser(String trxCode, String userId) {
    TransactionInProgress trx =
        mongoTemplate.findAndModify(
            Query.query(
                criteriaByTrxCode(trxCode)
                    .orOperator(criteriaByUserId(userId), criteriaByUserId(null))),
            new Update().set(Fields.userId, userId),
            TransactionInProgress.class);

    if(trx == null && mongoTemplate.exists(Query.query(criteriaByTrxCode(trxCode)), TransactionInProgress.class)){
      throw new ClientExceptionNoBody(HttpStatus.FORBIDDEN, "Transaction with trxCode [%s] is already assigned to another user".formatted(trxCode));
    }

    if (trx == null){
      throw new ClientExceptionNoBody(HttpStatus.NOT_FOUND, "Cannot find transaction with trxCode [%s]".formatted(trxCode));
    }

    return trx;
  }

  @Override
  public void updateTrxRejected(String id, List<String> rejectionReasons) {
    mongoTemplate.updateFirst(
        Query.query(Criteria.where(Fields.id).is(id)),
        new Update()
            .set(Fields.status, SyncTrxStatus.REJECTED)
            .set(Fields.rejectionReasons, rejectionReasons),
        TransactionInProgress.class);
  }

  @Override
  public void updateTrxIdentified(String id) {
    mongoTemplate.updateFirst(
        Query.query(Criteria.where(Fields.id).is(id)),
        new Update()
            .set(Fields.status, SyncTrxStatus.IDENTIFIED),
        TransactionInProgress.class);
  }

  private Criteria criteriaByTrxCode(String trxCode) {
    return Criteria.where(Fields.trxCode).is(trxCode);
  }

  private Criteria criteriaByUserId(String userId) {
    return Criteria.where(Fields.userId).is(userId);
  }
}
