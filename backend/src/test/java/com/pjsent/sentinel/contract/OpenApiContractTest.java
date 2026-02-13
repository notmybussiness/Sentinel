package com.pjsent.sentinel.contract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.yaml.snakeyaml.Yaml;

@SpringBootTest(properties = {
        "stock.market.alphavantage.api-key=test-key",
        "stock.market.finnhub.api-key=test-key",
        "jwt.secret=test-jwt-secret-key-for-testing-only",
        "cors.allowed-origins=http://localhost:3000",
        "kakao.oauth.client-id=test-kakao-client-id",
        "kakao.oauth.client-secret=test-kakao-client-secret",
        "kakao.oauth.redirect-uri=http://localhost:8080/api/v1/auth/kakao/callback",
        "spring.main.allow-bean-definition-overriding=true"
})
@AutoConfigureMockMvc
class OpenApiContractTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RedisConnectionFactory redisConnectionFactory;

    @MockBean
    private RedisTemplate<String, Object> redisTemplate;

    @MockBean
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Test
    @DisplayName("marks GET /market/prices as deprecated and exposes canonical write path")
    void shouldExposeExpectedMarketPathContracts() throws Exception {
        String response = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode root = objectMapper.readTree(response);
        JsonNode paths = root.path("paths");

        assertThat(paths.path("/api/v1/market/prices").isMissingNode()).isFalse();
        assertThat(paths.path("/api/v1/market/prices").path("get").path("deprecated").asBoolean()).isTrue();
        assertThat(paths.path("/api/v1/market/prices").path("post").isMissingNode()).isFalse();
        assertThat(paths.path("/api/v1/market/price/{symbol}/refresh").path("post").isMissingNode()).isFalse();
    }

    @Test
    @DisplayName("exposes ApiErrorResponse schema in OpenAPI components")
    void shouldExposeStandardApiErrorResponseSchema() throws Exception {
        String response = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode root = objectMapper.readTree(response);
        JsonNode schemas = root.path("components").path("schemas");

        assertThat(schemas.path("ApiErrorResponse").isMissingNode()).isFalse();
    }

    @Test
    @DisplayName("exports OpenAPI json/yaml snapshots")
    void shouldExportOpenApiSnapshots() throws Exception {
        String response = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode root = objectMapper.readTree(response);
        Path outputDir = Paths.get("..", "docs", "specs", "api");
        Files.createDirectories(outputDir);

        Path jsonPath = outputDir.resolve("openapi.json");
        Path yamlPath = outputDir.resolve("openapi.yaml");

        String prettyJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
        Files.writeString(jsonPath, prettyJson, StandardCharsets.UTF_8);

        @SuppressWarnings("unchecked")
        Map<String, Object> data = objectMapper.convertValue(root, Map.class);
        String yaml = new Yaml().dump(data);
        Files.writeString(yamlPath, yaml, StandardCharsets.UTF_8);

        assertThat(Files.exists(jsonPath)).isTrue();
        assertThat(Files.exists(yamlPath)).isTrue();
    }
}
