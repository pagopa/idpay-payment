package it.gov.pagopa.payment.utils;

import java.security.SecureRandom;
import java.util.function.Supplier;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TrxCodeGenUtil implements Supplier<String> {

  private final Integer trxCodeLength;

  public TrxCodeGenUtil(@Value("${app.trxCodeLength}") Integer trxCodeLength) {
    this.trxCodeLength = trxCodeLength;
  }

  @Override
  public String get() {
    return RandomStringUtils.random(trxCodeLength, 0, 0, true, true, null, new SecureRandom()).toLowerCase();
  }
}
