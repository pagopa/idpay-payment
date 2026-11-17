package it.gov.pagopa.payment.controller;

import it.gov.pagopa.payment.configuration.PaymentErrorManagerConfig;
import it.gov.pagopa.payment.repository.TestTransactionRepository;
import it.gov.pagopa.payment.service.payment.TestTransactionServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = TestTransactionControllerImpl.class,
        properties = "app.test-support.enabled=true",
        excludeAutoConfiguration = {UserDetailsServiceAutoConfiguration.class, SecurityAutoConfiguration.class})
@AutoConfigureMockMvc(addFilters = false)
@Import({TestTransactionServiceImpl.class, PaymentErrorManagerConfig.class})
class TestTransactionControllerTest {

    private static final String ENDPOINT = "/idpay/internal/test-support/transactions/{id}/end-date";
    private static final String END_DATE = "2026-11-20T23:59:59+01:00";
    private static final OffsetDateTime NEW_END_DATE = OffsetDateTime.parse(END_DATE);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TestTransactionRepository transactionRepository;

    @Test
    void shouldUpdateMatchingTransaction() throws Exception {
        when(transactionRepository.updateTrxEndDate("trx-id", "initiative-id", "user-id", NEW_END_DATE))
                .thenReturn(1);

        mockMvc.perform(validRequest())
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(transactionRepository).updateTrxEndDate("trx-id", "initiative-id", "user-id", NEW_END_DATE);
    }

    @Test
    void shouldReturnNotFoundWhenNoTransactionMatches() throws Exception {
        mockMvc.perform(validRequest())
                .andExpect(status().isNotFound());

        verify(transactionRepository).updateTrxEndDate("trx-id", "initiative-id", "user-id", NEW_END_DATE);
    }

    @ParameterizedTest
    @ValueSource(strings = {"invalid", "2026-11-20", "2026-11-20T23:59:59", "2026-02-30T23:59:59+01:00"})
    void shouldRejectInvalidTimestamp(String timestamp) throws Exception {
        mockMvc.perform(patch(ENDPOINT, "trx-id")
                        .param("initiativeId", "initiative-id")
                        .param("userId", "user-id")
                        .param("newEndDate", timestamp))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(transactionRepository);
    }

    @ParameterizedTest
    @ValueSource(strings = {"initiativeId", "userId", "newEndDate"})
    void shouldRequireEveryParameter(String missingParameter) throws Exception {
        MockHttpServletRequestBuilder request = patch(ENDPOINT, "trx-id");
        for (String parameter : List.of("initiativeId", "userId", "newEndDate")) {
            if (!parameter.equals(missingParameter)) {
                request.param(parameter, parameter.equals("newEndDate") ? END_DATE : "value");
            }
        }

        mockMvc.perform(request).andExpect(status().isBadRequest());

        verifyNoInteractions(transactionRepository);
    }

    private MockHttpServletRequestBuilder validRequest() {
        return patch(ENDPOINT, "trx-id")
                .param("initiativeId", "initiative-id")
                .param("userId", "user-id")
                .param("newEndDate", END_DATE);
    }
}
