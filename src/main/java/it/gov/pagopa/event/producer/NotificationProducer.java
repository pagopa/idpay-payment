package it.gov.pagopa.event.producer;

import it.gov.pagopa.payment.dto.NotificationQueueDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Component;

@Component
public class NotificationProducer {
    @Value("${spring.cloud.stream.bindings.paymentQueue-out-0.binder}")
    private String binder;
    @Autowired
    StreamBridge streamBridge;

    public void sendNotification(NotificationQueueDTO notificationQueueDTO) {
        streamBridge.send("paymentQueue-out-0", binder, notificationQueueDTO);
    }
}
