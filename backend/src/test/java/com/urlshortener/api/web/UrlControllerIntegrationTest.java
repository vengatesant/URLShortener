package com.urlshortener.api.web;

import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.urlshortener.api.dto.CreateUrlRequest;
import com.urlshortener.api.dto.UrlResponse;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Exercises the full stack (controller -&gt; service -&gt; Postgres) against a real Testcontainers
 * database rather than mocks, since the interesting failure modes here (unique-constraint races,
 * async click writes actually landing) only show up against a real engine.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
class UrlControllerIntegrationTest {

    @Container @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @Test
    void createRedirectAndTrackClickEndToEnd() throws Exception {
        String requestBody =
                objectMapper.writeValueAsString(
                        new CreateUrlRequest("https://example.com/very/long/path", null, null));

        MvcResult createResult =
                mockMvc
                        .perform(post("/api/urls").contentType(MediaType.APPLICATION_JSON).content(requestBody))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.shortCode").isNotEmpty())
                        .andReturn();

        UrlResponse created =
                objectMapper.readValue(createResult.getResponse().getContentAsString(), UrlResponse.class);

        mockMvc
                .perform(get("/r/{shortCode}", created.shortCode()))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://example.com/very/long/path"));

        // Click recording is async (fire-and-forget off the redirect thread); poll instead of
        // asserting immediately.
        await()
                .atMost(Duration.ofSeconds(3))
                .untilAsserted(
                        () ->
                                mockMvc
                                        .perform(get("/api/urls/{shortCode}/stats", created.shortCode()))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.totalClicks").value(1)));
    }

    @Test
    void rejectsDuplicateAlias() throws Exception {
        String requestBody =
                objectMapper.writeValueAsString(new CreateUrlRequest("https://example.com/one", "dup-alias", null));

        mockMvc
                .perform(post("/api/urls").contentType(MediaType.APPLICATION_JSON).content(requestBody))
                .andExpect(status().isCreated());

        String secondBody =
                objectMapper.writeValueAsString(new CreateUrlRequest("https://example.com/two", "dup-alias", null));

        mockMvc
                .perform(post("/api/urls").contentType(MediaType.APPLICATION_JSON).content(secondBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("ALIAS_TAKEN"));
    }

    @Test
    void rejectsNonHttpScheme() throws Exception {
        String requestBody =
                objectMapper.writeValueAsString(new CreateUrlRequest("javascript:alert(1)", null, null));

        mockMvc
                .perform(post("/api/urls").contentType(MediaType.APPLICATION_JSON).content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_URL"));
    }

    @Test
    void deactivatedLinkReturnsGoneNotNotFound() throws Exception {
        String requestBody =
                objectMapper.writeValueAsString(
                        new CreateUrlRequest("https://example.com/to-deactivate", "to-remove", null));

        mockMvc.perform(post("/api/urls").contentType(MediaType.APPLICATION_JSON).content(requestBody));

        mockMvc.perform(delete("/api/urls/{shortCode}", "to-remove")).andExpect(status().isNoContent());

        mockMvc.perform(get("/r/{shortCode}", "to-remove")).andExpect(status().isGone());

        mockMvc.perform(get("/r/{shortCode}", "never-existed-xyz")).andExpect(status().isNotFound());
    }
}
