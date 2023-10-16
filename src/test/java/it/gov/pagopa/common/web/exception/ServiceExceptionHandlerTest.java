package it.gov.pagopa.common.web.exception;

import it.gov.pagopa.common.web.exception.custom.BadRequestException;
import it.gov.pagopa.common.web.exception.custom.ForbiddenException;
import it.gov.pagopa.common.web.exception.custom.InternalServerErrorException;
import it.gov.pagopa.common.web.exception.custom.NotFoundException;
import it.gov.pagopa.common.web.exception.custom.TooManyRequestsException;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@ExtendWith(SpringExtension.class)
@WebMvcTest(value = {ServiceExceptionHandlerTest.TestController.class}, excludeAutoConfiguration = SecurityAutoConfiguration.class)
@ContextConfiguration(classes = {ServiceExceptionHandler.class, ServiceExceptionHandlerTest.TestController.class, ErrorManager.class})
class ServiceExceptionHandlerTest {

  @Autowired
  private MockMvc mockMvc;

  @SpyBean
  private TestController testControllerSpy;

  @RestController
  @Slf4j
  static class TestController {

    @GetMapping("/test")
    String testEndpoint() {
      return "OK";
    }
  }

  @Test
  void handleNotFoundException() throws Exception {
    Mockito.doThrow(new NotFoundException("NOT FOUND", "NOT_FOUND"))
        .when(testControllerSpy).testEndpoint();

    mockMvc.perform(MockMvcRequestBuilders.get("/test")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(MockMvcResultMatchers.status().isNotFound());
  }
  @Test
  void handleBadRequestException() throws Exception {
    Mockito.doThrow(new BadRequestException("BAD REQUEST", "BAD_REQUEST"))
        .when(testControllerSpy).testEndpoint();

    mockMvc.perform(MockMvcRequestBuilders.get("/test")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(MockMvcResultMatchers.status().isBadRequest());
  }

  @Test
  void handleForbiddenException() throws Exception {
    Mockito.doThrow(new ForbiddenException("FORBIDDEN", "FORBIDDEN"))
        .when(testControllerSpy).testEndpoint();

    mockMvc.perform(MockMvcRequestBuilders.get("/test")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(MockMvcResultMatchers.status().isForbidden());
  }

  @Test
  void handleInternalServerErrorException() throws Exception {
    Mockito.doThrow(new InternalServerErrorException("INTERNAL SERVER ERROR", "INTERNAL_SERVER_ERROR"))
        .when(testControllerSpy).testEndpoint();

    mockMvc.perform(MockMvcRequestBuilders.get("/test")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(MockMvcResultMatchers.status().isInternalServerError());
  }

  @Test
  void handleTooManyRequestsException() throws Exception {
    Mockito.doThrow(new TooManyRequestsException("TOO MANY REQUESTS", "TOO_MANY_REQUESTS"))
        .when(testControllerSpy).testEndpoint();

    mockMvc.perform(MockMvcRequestBuilders.get("/test")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(MockMvcResultMatchers.status().isTooManyRequests());
  }
}
