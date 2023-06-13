package it.gov.pagopa.payment.service.qrcode.expired;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class QRCodeExpirationServiceImpl {

    private final QRCodeAuthorizationExpiredService authorizationExpiredService;
    private final QRCodeCancelExpiredService cancelExpiredService;

    public QRCodeExpirationServiceImpl(QRCodeAuthorizationExpiredService authorizationExpiredService,
                                       QRCodeCancelExpiredService cancelExpiredService) {
        this.authorizationExpiredService = authorizationExpiredService;
        this.cancelExpiredService = cancelExpiredService;
    }

    @Scheduled(cron = "${app.qrCode.expirations.schedule.authorizationExpired}")
    void scheduleAuthorizationExpired() {
        log.debug("[EXPIRED_QR_CODE][TRANSACTION_AUTHORIZATION_EXPIRED] Starting schedule to handle transactions with authorization expired");
    }

    @Scheduled(cron = "${app.qrCode.expirations.schedule.cancelExpired}")
    void scheduleCancelExpired() {
        log.debug("[EXPIRED_QR_CODE][TRANSACTION_CANCEL_EXPIRED] Starting schedule to handle transactions with authorization expired");

    }

}
