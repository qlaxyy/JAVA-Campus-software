package edu.seu.vcampus.server.module.hospital;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.net.httpserver.HttpServer;
import edu.seu.vcampus.common.hospital.TriageFollowUpAnswer;
import edu.seu.vcampus.common.hospital.TriageRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DashScopeHospitalAiTriageClientTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @TempDir
    Path temporaryDirectory;

    @Test
    void sendsStructuredConversationAndParsesStructuredAnswer() throws Exception {
        AtomicReference<JsonNode> capturedBody = new AtomicReference<>();
        AtomicReference<String> authorization = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/chat/completions", exchange -> {
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            capturedBody.set(mapper.readTree(exchange.getRequestBody()));
            ObjectNode analysis = mapper.createObjectNode();
            analysis.put("followUpRequired", true);
            analysis.put("followUpQuestion", "是否伴有发热？");
            analysis.putArray("candidates");
            ObjectNode response = mapper.createObjectNode();
            response.putArray("choices").addObject().putObject("message")
                    .put("content", mapper.writeValueAsString(analysis));
            byte[] bytes = mapper.writeValueAsBytes(response);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        try {
            URI endpoint = URI.create("http://127.0.0.1:" + server.getAddress().getPort()
                    + "/chat/completions");
            DashScopeHospitalAiTriageClient client =
                    DashScopeHospitalAiTriageClient.forTesting(
                            endpoint, "test-key", "test-model", Duration.ofSeconds(2));

            HospitalAiTriageAnalysis result = client.analyze(
                    new TriageRequest("咳嗽", false, List.of(
                            new TriageFollowUpAnswer("持续多久？", "两天"))),
                    List.of(new HospitalDepartment(
                            "dept-respiratory", "呼吸内科", null, true, true)));

            assertEquals("Bearer test-key", authorization.get());
            assertEquals("test-model", capturedBody.get().path("model").asText());
            assertFalse(capturedBody.get().path("enable_thinking").asBoolean(true));
            assertEquals("json_schema",
                    capturedBody.get().path("response_format").path("type").asText());
            assertFalse(capturedBody.get().path("response_format").path("json_schema")
                    .path("schema").path("properties").has("urgent"));
            String systemMessage = capturedBody.get().path("messages").path(0)
                    .path("content").asText();
            assertTrue(systemMessage.contains("危险信号由服务器的固定安全问题处理"));
            assertTrue(systemMessage.contains("每次只问一件事"));
            String userMessage = capturedBody.get().path("messages").path(1)
                    .path("content").asText();
            assertTrue(userMessage.contains("dept-respiratory"));
            assertTrue(userMessage.contains("持续多久"));
            assertEquals("是否伴有发热？", result.followUpQuestion());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void remainsDisabledUntilExplicitlyEnabledWithAKey() {
        Properties properties = new Properties();
        HospitalAiTriageClient disabled =
                DashScopeHospitalAiTriageClient.fromConfiguration(Map.of(), properties);
        assertFalse(disabled.isEnabled());

        properties.setProperty("vcampus.triage.ai.enabled", "true");
        HospitalAiTriageClient missingKey =
                DashScopeHospitalAiTriageClient.fromConfiguration(Map.of(), properties);
        assertFalse(missingKey.isEnabled());

        properties.setProperty("vcampus.triage.ai.apiKey", "secret");
        HospitalAiTriageClient enabled =
                DashScopeHospitalAiTriageClient.fromConfiguration(Map.of(), properties);
        assertTrue(enabled.isEnabled());
    }

    @Test
    void loadsAllRequiredSettingsFromOneLocalPropertiesFile() throws Exception {
        Path path = temporaryDirectory.resolve("hospital-ai.properties");
        Files.writeString(path, """
                enabled=true
                apiKey=local-secret
                baseUrl=https://workspace.example.com/compatible-mode/v1
                model=qwen-test
                """, StandardCharsets.UTF_8);

        Properties fileConfiguration = HospitalAiTriageConfiguration.load(path);
        HospitalAiTriageClient client = DashScopeHospitalAiTriageClient.fromConfiguration(
                Map.of(), new Properties(), fileConfiguration);

        assertTrue(client.isEnabled());
        assertEquals("local-secret", fileConfiguration.getProperty("apiKey"));
    }
}
