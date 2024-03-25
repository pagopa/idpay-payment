package it.gov.pagopa.common.kafka.service;

public interface ErrorNotifierService {
    boolean notify(ErrorNotifierInfoDTO errorNotifierInfoDTO);
}