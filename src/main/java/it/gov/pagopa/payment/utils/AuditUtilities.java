package it.gov.pagopa.payment.utils;

import it.gov.pagopa.common.utils.AuditLogger;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@AllArgsConstructor
public class AuditUtilities {

    private static final String CEF = String.format("CEF:0|PagoPa|IDPAY|1.0|7|User interaction|2| event=Payment dstip=%s", AuditLogger.SRCIP);
    private static final String CEF_BASE_PATTERN = CEF + " msg={}";
    private static final String CEF_PATTERN_INITIATIVE = CEF_BASE_PATTERN + " cs1Label=initiativeId cs1={}";
    private static final String CEF_PATTERN_TRXID_TRXCODE = CEF_PATTERN_INITIATIVE + " cs2Label=trxId cs2={} cs3Label=trxCode cs3={}";
    private static final String CEF_PATTERN_TRXID_TRXCODE_MERCHANTID = CEF_PATTERN_TRXID_TRXCODE + " cs4Label=merchantId cs4={}";
    private static final String CEF_PATTERN_USER = CEF_PATTERN_TRXID_TRXCODE + " suser={}";
    private static final String CEF_PATTERN_TRXID_TRXCODE_CHANNEL_USER = CEF_PATTERN_TRXID_TRXCODE + " cs4Label=channel cs4={} suser={}";
    private static final String CEF_PATTERN_REWARD_REJECTIONS = CEF_PATTERN_USER + " cs4Label=reward cs4={} cs5Label=rejectionReasons cs5={}";
    private static final String CEF_PATTERN_MERCHANT_REWARD_REJECTIONS = CEF_PATTERN_TRXID_TRXCODE_MERCHANTID + " cs5Label=reward cs5={} cs6Label=rejectionReasons cs6={}";
    private static final String CEF_PATTERN_REWARD_REJECTIONS_MERCHANTID = CEF_PATTERN_USER + " cs4Label=reward cs4={} cs5Label=rejectionReasons cs5={} cs6Label=merchantId cs6={}";
    private static final String CEF_PATTERN_INITIATIVE_MERCHANTID = CEF_PATTERN_INITIATIVE + " cs2Label=merchantId cs2={}";
    private static final String CEF_PATTERN_TRXCODE_USERID = CEF_BASE_PATTERN + " cs1Label=trxCode cs1={} suser={}";
    private static final String CEF_PATTERN_TRXCODE_MERCHANTID = CEF_BASE_PATTERN + " cs1Label=trxCode cs1={} cs2Label=merchantId cs2={}";
    private static final String CEF_PATTERN_TRXID_MERCHANTID = CEF_BASE_PATTERN + " cs1Label=trxId cs1={} cs2Label=merchantId cs2={}";
    private static final String CEF_PATTERN_TRXID_USERID = CEF_BASE_PATTERN + " cs1Label=trxId cs1={} cs2Label=userId cs2={}";
    private static final String CEF_PATTERN_EXPIRED_TRX = CEF_PATTERN_USER + " cs4Label=flowCause cs4={}" ;
    private static final String CEF_PATTERN_USER_INITIATIVE = CEF_BASE_PATTERN + " suser={} cs1Label=initiativeId cs1={}";


    // region createTransaction
    public void logCreatedTransaction(String initiativeId, String trxId, String trxCode, String merchantId) {
        AuditLogger.logAuditString(
                CEF_PATTERN_TRXID_TRXCODE_MERCHANTID,
                "Transaction created", initiativeId, trxId, trxCode, merchantId
        );
    }

    public void logErrorCreatedTransaction(String initiativeId, String merchantId) {
        AuditLogger.logAuditString(
                CEF_PATTERN_INITIATIVE_MERCHANTID,
                "Transaction created - KO", initiativeId, merchantId
        );
    }
    // endregion

    // region preAuthPayment relateUser
    public void logRelatedUserToTransaction(String initiativeId, String trxId, String trxCode, String userId, String channel) {
        AuditLogger.logAuditString(
                CEF_PATTERN_TRXID_TRXCODE_CHANNEL_USER,
                "User related to transaction", initiativeId, trxId, trxCode, userId, channel
        );
    }

    public void logErrorRelatedUserToTransaction(String trxCode, String userId) {
        AuditLogger.logAuditString(
                CEF_PATTERN_TRXCODE_USERID,
                "User related to transaction - KO", trxCode, userId
        );
    }
    // endregion

    // region preAuthPayment previewPayment
    public void logPreviewTransaction(String initiativeId, String trxId, String trxCode, String userId, String channel) {
        AuditLogger.logAuditString(
                CEF_PATTERN_TRXID_TRXCODE_CHANNEL_USER,
                "User request preview the transaction", initiativeId, trxId, trxCode, userId, channel
        );
    }
    public void logErrorPreviewTransaction(String initiativeId, String trxId, String trxCode, String userId, String channel) {
        AuditLogger.logAuditString(
                CEF_PATTERN_TRXID_TRXCODE_CHANNEL_USER,
                "User request preview the transaction - KO", initiativeId, trxId, trxCode, userId, channel
        );
    }
    // endregion

    // region authPayment
    public void logAuthorizedPayment(String initiativeId, String trxId, String trxCode, String userId, Long reward, List<String> rejectionReasons) {
        AuditLogger.logAuditString(
                CEF_PATTERN_REWARD_REJECTIONS,
                "User authorized the transaction", initiativeId, trxId, trxCode, userId, reward.toString(), rejectionReasons.toString()
        );
    }

    public void logBarCodeAuthorizedPayment(String initiativeId, String trxId, String trxCode, String merchantId, Long reward, List<String> rejectionReasons) {
        AuditLogger.logAuditString(
                CEF_PATTERN_MERCHANT_REWARD_REJECTIONS,
                "Merchant authorized the transaction", initiativeId, trxId, trxCode, merchantId, reward.toString(), rejectionReasons.toString()
        );
    }

    public void logErrorAuthorizedPayment(String trxCode, String userId) {
        AuditLogger.logAuditString(
                CEF_PATTERN_TRXCODE_USERID,
                "User authorized the transaction - KO", trxCode, userId
        );
    }

    public void logBarCodeErrorAuthorizedPayment(String trxCode, String merchantId){
        AuditLogger.logAuditString(
                CEF_PATTERN_TRXCODE_MERCHANTID,
                "Merchant authorized the transaction - KO", trxCode, merchantId
        );
    }
    // endregion

    // region confirmPayment
    public void logConfirmedPayment(String initiativeId, String trxId, String trxCode, String userId, Long reward, List<String> rejectionReasons, String merchantId) {
        AuditLogger.logAuditString(
                CEF_PATTERN_REWARD_REJECTIONS_MERCHANTID,
                "Merchant confirmed the transaction", initiativeId, trxId, trxCode, userId, reward.toString(), rejectionReasons.toString(), merchantId
        );
    }

    public void logErrorConfirmedPayment(String trxId, String merchantId) {
        AuditLogger.logAuditString(
                CEF_PATTERN_TRXID_MERCHANTID,
                "Merchant confirmed the transaction - KO", trxId, merchantId
        );
    }
    // endregion

    // region confirmPayment
    public void logCancelTransaction(String initiativeId, String trxId, String trxCode, String userId, Long reward, List<String> rejectionReasons, String merchantId) {
        AuditLogger.logAuditString(
                CEF_PATTERN_REWARD_REJECTIONS_MERCHANTID,
                "Merchant cancelled the transaction", initiativeId, trxId, trxCode, userId, reward.toString(), rejectionReasons.toString(), merchantId
        );
    }

    public void logErrorCancelTransaction(String trxId, String merchantId) {
        AuditLogger.logAuditString(
                CEF_PATTERN_TRXID_MERCHANTID,
                "Merchant cancelled the transaction - KO", trxId, merchantId
        );
    }

    public void logUnrelateTransaction(String initiativeId, String trxId, String trxCode, String userId, Long reward, List<String> rejectionReasons) {
        AuditLogger.logAuditString(
                CEF_PATTERN_REWARD_REJECTIONS,
                "User unrelated the transaction", initiativeId, trxId, trxCode, userId, reward.toString(), rejectionReasons.toString()
        );
    }

    public void logErrorUnrelateTransaction(String trxCode, String userId) {
        AuditLogger.logAuditString(
                CEF_PATTERN_TRXID_USERID,
                "User unrelated the transaction - KO", trxCode, userId
        );
    }
    // endregion

    // region expired transaction
    public void logExpiredTransaction(String initiativeId, String trxId, String trxCode, String userId, String flowCause){
        AuditLogger.logAuditString(CEF_PATTERN_EXPIRED_TRX,
                "Expire transaction processed", initiativeId, trxId, trxCode, userId, flowCause);
    }

    public void logErrorExpiredTransaction(String initiativeId, String trxId, String trxCode, String userId, String flowCause){
        AuditLogger.logAuditString(CEF_PATTERN_EXPIRED_TRX,
                "Expire transaction processed - KO", initiativeId, trxId, trxCode, userId, flowCause);
    }
    // endregion

    // region transaction in progress delete
    public void logDeleteTransactions(String userId, String initiativeId){
        AuditLogger.logAuditString(CEF_PATTERN_USER_INITIATIVE,
                "Transactions in progress deleted", userId, initiativeId);
    }
    // endregion
}
