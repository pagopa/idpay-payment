package it.gov.pagopa.payment.utils;

import java.util.function.Supplier;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.stereotype.Service;

@Service
public class TrxCodeGenUtil implements Supplier<String> {

  @Override
  public String get() {
    return RandomStringUtils.randomAlphanumeric(6).toLowerCase();
  }
}
