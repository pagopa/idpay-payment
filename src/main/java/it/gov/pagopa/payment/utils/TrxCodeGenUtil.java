package it.gov.pagopa.payment.utils;

import java.security.SecureRandom;
import java.util.function.Supplier;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.stereotype.Service;

@Service
public class TrxCodeGenUtil implements Supplier<String> {

  @Override
  public String get() {
    return RandomStringUtils.random(6, 0, 0, true, true, null, new SecureRandom()).toLowerCase();
  }
}
