package it.gov.pagopa.payment.service.payment;

import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.utils.TrxCodeGenUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class TransactionInProgressServiceImpl implements TransactionInProgressService{
    private final TransactionInProgressRepository transactionInProgressRepository;
    private final TrxCodeGenUtil trxCodeGenUtil;

    public TransactionInProgressServiceImpl(TransactionInProgressRepository transactionInProgressRepository, TrxCodeGenUtil trxCodeGenUtil) {
        this.transactionInProgressRepository = transactionInProgressRepository;
        this.trxCodeGenUtil = trxCodeGenUtil;
    }

    @Override
    public void generateTrxCodeAndSave(TransactionInProgress trx, String flowName) {
        long retry = 1;
        while (transactionInProgressRepository.createIfExists(trx, trxCodeGenUtil.get()).getUpsertedId()
                == null) {
            log.info(
                    "[{}] [GENERATE_TRX_CODE] Duplicate hit: generating new trxCode [Retry #{}]",
                    flowName,
                    retry);
        }
    }
}
