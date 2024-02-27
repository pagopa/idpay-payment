package it.gov.pagopa.payment.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
class TrxCodeGenUtilTest {

    @Mock
    private TrxCodeGenUtil trxCodeGenUtil;

    private final Integer TRXCODELENGTH = 8;

    @BeforeEach
    public void setUp(){
        trxCodeGenUtil = new TrxCodeGenUtil(TRXCODELENGTH);
    }
     @Test
    void createRandomTrxCode() {
         String trxCode = trxCodeGenUtil.get();
         assertNotNull(trxCode);
         assertEquals(TRXCODELENGTH,trxCode.length());
    }

}
