package it.gov.pagopa.payment.configuration;

import it.gov.pagopa.common.web.exception.custom.ServiceException;
import it.gov.pagopa.payment.exception.custom.badrequest.OperationNotAllowedException;
import it.gov.pagopa.payment.exception.custom.badrequest.TransactionInvalidException;
import it.gov.pagopa.payment.exception.custom.forbidden.BudgetExhaustedException;
import it.gov.pagopa.payment.exception.custom.forbidden.InitiativeInvalidException;
import it.gov.pagopa.payment.exception.custom.forbidden.MerchantOrAcquirerNotAllowedException;
import it.gov.pagopa.payment.exception.custom.forbidden.PinBlockInvalidException;
import it.gov.pagopa.payment.exception.custom.forbidden.TransactionAlreadyAuthorizedException;
import it.gov.pagopa.payment.exception.custom.forbidden.TransactionRejectedException;
import it.gov.pagopa.payment.exception.custom.forbidden.UserNotAllowedException;
import it.gov.pagopa.payment.exception.custom.forbidden.UserNotOnboardedException;
import it.gov.pagopa.payment.exception.custom.forbidden.UserSuspendedException;
import it.gov.pagopa.payment.exception.custom.notfound.IdpaycodeNotFoundException;
import it.gov.pagopa.payment.exception.custom.notfound.InitiativeNotfoundException;
import it.gov.pagopa.payment.exception.custom.notfound.MerchantNotFoundException;
import it.gov.pagopa.payment.exception.custom.notfound.TransactionNotFoundOrExpiredException;
import it.gov.pagopa.payment.exception.custom.servererror.InternalServerErrorException;
import it.gov.pagopa.payment.exception.custom.servererror.MerchantInvocationException;
import it.gov.pagopa.payment.exception.custom.servererror.PDVInvocationException;
import it.gov.pagopa.payment.exception.custom.servererror.PaymentInstrumentInvocationException;
import it.gov.pagopa.payment.exception.custom.servererror.RewardCalculatorInvocationException;
import it.gov.pagopa.payment.exception.custom.servererror.WalletInvocationException;
import it.gov.pagopa.payment.exception.custom.toomanyrequests.TooManyRequestsException;
import java.util.HashMap;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;

@Configuration
public class ServiceExceptionConfig {

  @Bean
  public Map<Class<? extends ServiceException>, HttpStatus> serviceExceptionMapper() {
    Map<Class<? extends ServiceException>, HttpStatus> exceptionMap = new HashMap<>();

    // BadRequest
    exceptionMap.put(OperationNotAllowedException.class, HttpStatus.BAD_REQUEST);
    exceptionMap.put(TransactionInvalidException.class, HttpStatus.BAD_REQUEST);

    // Forbidden
    exceptionMap.put(BudgetExhaustedException.class, HttpStatus.FORBIDDEN);
    exceptionMap.put(InitiativeInvalidException.class, HttpStatus.FORBIDDEN);
    exceptionMap.put(MerchantOrAcquirerNotAllowedException.class, HttpStatus.FORBIDDEN);
    exceptionMap.put(PinBlockInvalidException.class, HttpStatus.FORBIDDEN);
    exceptionMap.put(TransactionAlreadyAuthorizedException.class, HttpStatus.FORBIDDEN);
    exceptionMap.put(TransactionRejectedException.class, HttpStatus.FORBIDDEN);
    exceptionMap.put(UserNotAllowedException.class, HttpStatus.FORBIDDEN);
    exceptionMap.put(UserNotOnboardedException.class, HttpStatus.FORBIDDEN);
    exceptionMap.put(UserSuspendedException.class, HttpStatus.FORBIDDEN);

    // NotFound
    exceptionMap.put(IdpaycodeNotFoundException.class, HttpStatus.NOT_FOUND);
    exceptionMap.put(InitiativeNotfoundException.class, HttpStatus.NOT_FOUND);
    exceptionMap.put(MerchantNotFoundException.class, HttpStatus.NOT_FOUND);
    exceptionMap.put(TransactionNotFoundOrExpiredException.class, HttpStatus.NOT_FOUND);

    // InternalServerError
    exceptionMap.put(InternalServerErrorException.class, HttpStatus.INTERNAL_SERVER_ERROR);
    exceptionMap.put(MerchantInvocationException.class, HttpStatus.INTERNAL_SERVER_ERROR);
    exceptionMap.put(PaymentInstrumentInvocationException.class, HttpStatus.INTERNAL_SERVER_ERROR);
    exceptionMap.put(PDVInvocationException.class, HttpStatus.INTERNAL_SERVER_ERROR);
    exceptionMap.put(RewardCalculatorInvocationException.class, HttpStatus.INTERNAL_SERVER_ERROR);
    exceptionMap.put(WalletInvocationException.class, HttpStatus.INTERNAL_SERVER_ERROR);

    // TooManyRequests
    exceptionMap.put(TooManyRequestsException.class, HttpStatus.TOO_MANY_REQUESTS);

    return exceptionMap;
  }

}
