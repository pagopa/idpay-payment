package it.gov.pagopa.payment.service;

import it.gov.pagopa.payment.dto.event.QueueCommandOperationDTO;

public interface ProcessConsumerService {
    void processCommand(QueueCommandOperationDTO queueCommandOperationDTO);
}
