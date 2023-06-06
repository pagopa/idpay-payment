package it.gov.pagopa.payment.connector.decrypt;

import it.gov.pagopa.payment.dto.DecryptCfDTO;
import org.springframework.stereotype.Service;

@Service
public interface DecryptRestConnector {

  DecryptCfDTO getPiiByToken(String token);
}
