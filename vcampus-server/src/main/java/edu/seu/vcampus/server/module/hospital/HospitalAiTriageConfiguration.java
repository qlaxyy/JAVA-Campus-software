package edu.seu.vcampus.server.module.hospital;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;

/** Loads local AI settings without ever persisting or logging credentials. */
final class HospitalAiTriageConfiguration {

    private static final Path DEFAULT_PATH = Path.of("config", "hospital-ai.properties");

    private HospitalAiTriageConfiguration() {
    }

    static Properties load(Map<String, String> environment, Properties systemProperties) {
        String configuredPath = systemProperties.getProperty(
                "vcampus.triage.ai.config",
                environment.getOrDefault("VCAMPUS_TRIAGE_AI_CONFIG", DEFAULT_PATH.toString()));
        return load(Path.of(configuredPath));
    }

    static Properties load(Path path) {
        Properties configuration = new Properties();
        if (!Files.isRegularFile(path)) {
            return configuration;
        }
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            configuration.load(reader);
        } catch (IOException | IllegalArgumentException exception) {
            return new Properties();
        }
        return configuration;
    }
}
