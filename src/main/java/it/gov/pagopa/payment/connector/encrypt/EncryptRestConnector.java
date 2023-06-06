package it.gov.pagopa.payment.connector.encrypt;

import it.gov.pagopa.payment.dto.CFDTO;
import it.gov.pagopa.payment.dto.EncryptedCfDTO;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

@Service
public interface EncryptRestConnector {

  EncryptedCfDTO upsertToken(@RequestBody CFDTO body);
}
