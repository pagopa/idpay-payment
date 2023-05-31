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
    private static final String CEF_PATTERN_TRXCODE = CEF_PATTERN_INITIATIVE + " cs2Label=trxCode cs2={}";
    private static final String CEF_PATTERN_TRXCODE_MERCHANTID = CEF_PATTERN_TRXCODE + " cs3Label=merchantId cs3={}";
    private static final String CEF_PATTERN_USER = CEF_PATTERN_TRXCODE + " suser={}";
    private static final String CEF_PATTERN_REWARD_REJECIONS = CEF_PATTERN_USER + " cs3Label=reward cs3={} cs4Label=rejectionReasons cs4={}";
    private static final String CEF_PATTERN_REWARD_REJECIONS_MERCHANTID = CEF_PATTERN_USER + " cs3Label=reward cs3={} cs4Label=rejectionReasons cs4={} cs5Label=merchantId cs5={}";
    private static final String CEF_PATTERN_INITIATIVE_MERCHANTID = CEF_PATTERN_INITIATIVE + " cs2Label=merchantId cs2={}";
    private static final String CEF_PATTERN_TRXCODE_USERID = CEF_BASE_PATTERN + " cs1Label=trxCode cs1={} suser={}";
    private static final String CEF_PATTERN_TRXID_MERCHANTID = CEF_BASE_PATTERN + " cs1Label=trxId cs1={} cs2Label=merchantId cs2={}";

    // region createTransaction
    public void logCreatedTransaction(String initiativeId, String trxCode, String merchantId) {
        AuditLogger.logAuditString(
                CEF_PATTERN_TRXCODE_MERCHANTID,
                "Transaction created", initiativeId, trxCode, merchantId
        );
    }

    public void logErrorCreatedTransaction(String initiativeId, String merchantId) {
        AuditLogger.logAuditString(
                CEF_PATTERN_INITIATIVE_MERCHANTID,
                "Transaction created - KO", initiativeId, merchantId
        );
    }
    // endregion

    // region relateUser
    public void logRelatedUserToTransaction(String initiativeId, String trxCode, String userId) {
        AuditLogger.logAuditString(
                CEF_PATTERN_USER,
                "User related to transaction", initiativeId, trxCode, userId
        );
    }

    public void logErrorRelatedUserToTransaction(String trxCode, String userId) {
        AuditLogger.logAuditString(
                CEF_PATTERN_TRXCODE_USERID,
                "User related to transaction - KO", trxCode, userId
        );
    }
    // endregion

    // region authPayment
    public void logAuthorizedPayment(String initiativeId, String trxCode, String userId, Long reward, List<String> rejectionReasons) {
        AuditLogger.logAuditString(
                CEF_PATTERN_REWARD_REJECIONS,
                "User authorized the transaction", initiativeId, trxCode, userId, reward.toString(), rejectionReasons.toString()
        );
    }

    public void logErrorAuthorizedPayment(String trxCode, String userId) {
        AuditLogger.logAuditString(
                CEF_PATTERN_TRXCODE_USERID,
                "User authorized the transaction - KO", trxCode, userId
        );
    }
    // endregion

    // region confirmPayment
    public void logConfirmedPayment(String initiativeId, String trxCode, String userId, Long reward, List<String> rejectionReasons, String merchantId) {
        AuditLogger.logAuditString(
                CEF_PATTERN_REWARD_REJECIONS_MERCHANTID,
                "Merchant confirmed the transaction", initiativeId, trxCode, userId, reward.toString(), rejectionReasons.toString(), merchantId
        );
    }

    public void logErrorConfirmedPayment(String trxId, String merchantId) {
        AuditLogger.logAuditString(
                CEF_PATTERN_TRXID_MERCHANTID,
                "Merchant confirmed the transaction - KO", trxId, merchantId
        );
    }
    // endregion
}