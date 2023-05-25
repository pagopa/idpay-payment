package it.gov.pagopa.payment.utils;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

@Component
@AllArgsConstructor
@Slf4j(topic = "AUDIT")
public class AuditUtilities {
    public static final String SRCIP;

    static {
        String srcIp;
        try {
            srcIp = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            log.error("Cannot determine the ip of the current host", e);
            srcIp = "UNKNOWN";
        }

        SRCIP = srcIp;
    }

    private static final String CEF = String.format("CEF:0|PagoPa|IDPAY|1.0|7|User interaction|2| event=Payment dstip=%s", SRCIP);
    private static final String CEF_BASE_PATTERN = CEF + " msg={}";
    private static final String CEF_PATTERN_INITIATIVE = CEF_BASE_PATTERN + " cs1Label=initiativeId cs1={}";
    private static final String CEF_PATTERN_TRXCODE = CEF_PATTERN_INITIATIVE + " cs2Label=trxCode cs2={}";
    private static final String CEF_PATTERN_TRXCODE_MERCHANTID = CEF_PATTERN_TRXCODE + " cs3Label=merchantId cs3={}";
    private static final String CEF_PATTERN_USER = CEF_PATTERN_TRXCODE + " suser={}";
    private static final String CEF_PATTERN_REWARD_REJECIONS = CEF_PATTERN_USER + " cs3Label=reward cs3={} cs4Label=rejectionReasons cs4={}";
    private static final String CEF_PATTERN_REWARD_REJECIONS_MERCHANTID = CEF_PATTERN_USER + " cs3Label=reward cs3={} cs4Label=rejectionReasons cs4={} cs5Label=merchantId cs5={}";

    private void logAuditString(String pattern, String... parameters) {
        log.info(pattern, (Object[]) parameters);
    }

    public void logCreatedTransaction(String initiativeId, String trxCode, String merchantId) {
        logAuditString(
                CEF_PATTERN_TRXCODE_MERCHANTID,
                "Transaction created", initiativeId, trxCode, merchantId
        );
    }

    public void logRelatedUserToTransaction(String initiativeId, String trxCode, String userId) {
        logAuditString(
                CEF_PATTERN_USER,
                "User related to transaction", initiativeId, trxCode, userId
        );
    }

    public void logAuthorizedPayment(String initiativeId, String trxCode, String userId, Long reward, List<String> rejectionReasons) {
        logAuditString(
                CEF_PATTERN_REWARD_REJECIONS,
                "User authorized the transaction", initiativeId, trxCode, userId, reward.toString(), rejectionReasons.toString()
        );
    }

    public void logConfirmedPayment(String initiativeId, String trxCode, String userId, Long reward, List<String> rejectionReasons, String merchantId) {
        logAuditString(
                CEF_PATTERN_REWARD_REJECIONS_MERCHANTID,
                "Merchant confirmed the transaction", initiativeId, trxCode, userId, reward.toString(), rejectionReasons.toString(), merchantId
        );
    }
}
