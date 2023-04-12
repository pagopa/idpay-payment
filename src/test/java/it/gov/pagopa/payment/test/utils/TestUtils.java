package it.gov.pagopa.payment.test.utils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Assertions;

public class TestUtils {

  private TestUtils() {}

  public static void checkNotNullFields(Object o, String... excludedFields) {
    Set<String> excludedFieldsSet = new HashSet<>(Arrays.asList(excludedFields));
    org.springframework.util.ReflectionUtils.doWithFields(o.getClass(),
        f -> {
          f.setAccessible(true);
          Assertions.assertNotNull(f.get(o), "The field %s of the input object of type %s is null!".formatted(f.getName(), o.getClass()));
        },
        f -> !excludedFieldsSet.contains(f.getName()));

  }
}
