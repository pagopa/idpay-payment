package it.gov.pagopa.payment.service.payment.expired;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class QRCodeExpirationServiceImpl implements QRCodeExpirationService {

    private final QRCodeAuthorizationExpiredService authorizationExpiredService;
    private final QRCodeCancelExpiredService cancelExpiredService;

    public QRCodeExpirationServiceImpl(QRCodeAuthorizationExpiredService authorizationExpiredService,
                                       QRCodeCancelExpiredService cancelExpiredService) {
        this.authorizationExpiredService = authorizationExpiredService;
        this.cancelExpiredService = cancelExpiredService;
    }

    @Scheduled(cron = "${app.qrCode.expirations.schedule.authorizationExpired}")
    void scheduleAuthorizationExpired() {
        log.info("[EXPIRED_QR_CODE][TRANSACTION_AUTHORIZATION_EXPIRED] Starting schedule to handle transactions with authorization expired");
        Long count = authorizationExpiredService.execute();
        log.info("[EXPIRED_QR_CODE][TRANSACTION_AUTHORIZATION_EXPIRED] Found {} expired transactions", count);
    }

    @Override
    public Long forceAuthorizationTrxExpiration(String initiativeId) {
        return authorizationExpiredService.forceExpiration(initiativeId);
    }

    @Scheduled(cron = "${app.qrCode.expirations.schedule.cancelExpired}")
    void scheduleCancelExpired() {
        log.info("[EXPIRED_QR_CODE][TRANSACTION_CANCEL_EXPIRED] Starting schedule to handle transactions with cancel expired");
        Long count = cancelExpiredService.execute();
        log.info("[EXPIRED_QR_CODE][TRANSACTION_CANCEL_EXPIRED] Found {} expired transactions", count);
    }

    @Override
    public Long forceConfirmTrxExpiration(String initiativeId) {
        return cancelExpiredService.forceExpiration(initiativeId);
    }
}
