package it.gov.pagopa.payment.service.payment;

import it.gov.pagopa.payment.entity.Transaction;
import it.gov.pagopa.payment.repository.TransactionRepository;
import it.gov.pagopa.payment.utils.TrxCodeGenUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository repository;
    private final TrxCodeGenUtil trxCodeGenUtil;

    public TransactionServiceImpl(TransactionRepository repository,
                                  TrxCodeGenUtil trxCodeGenUtil){
        this.repository = repository;
        this.trxCodeGenUtil = trxCodeGenUtil;
    }

    @Override
    public void generateTrxCodeAndSave(Transaction trx, String flowName) {
        for (int retry = 0; retry < 5; retry++) {
            //String trxCode = trxCodeGenUtil.get();
            //trx.setTrxCode(trxCode);
            try {
                repository.save(trx);
                return;
            } catch (DataIntegrityViolationException e) {
                log.info("[{}] duplicate trxCode, retry {}", flowName, retry);
            }
        }
    }

}
