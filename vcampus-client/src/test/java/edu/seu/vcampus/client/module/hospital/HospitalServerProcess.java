package edu.seu.vcampus.client.module.hospital;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Starts an isolated server JVM for hospital integration tests. */
final class HospitalServerProcess implements AutoCloseable {

    private static final Duration START_TIMEOUT = Duration.ofSeconds(30);
    private static final long POLL_MILLIS = 100;
    private static final long STOP_TIMEOUT_SECONDS = 5;
    private static final Pattern STARTED_PORT =
            Pattern.compile("Virtual Campus server started on port (\\d+)\\.");
    private static final int LOG_TAIL_LINES = 40;
    private static final int LOG_TAIL_CHARACTERS = 8_000;

    private final Process process;
    private final int port;

    private HospitalServerProcess(Process process, int port) {
        this.process = process;
        this.port = port;
    }

    static HospitalServerProcess start(Path databasePath, Path logPath) throws Exception {
        Path absoluteDatabasePath = databasePath.toAbsolutePath().normalize();
        Path absoluteLogPath = logPath.toAbsolutePath().normalize();
        Path logDirectory = absoluteLogPath.getParent();
        if (logDirectory != null) {
            Files.createDirectories(logDirectory);
        }

        Process process;
        try {
            process = new ProcessBuilder(
                    javaExecutable(),
                    "-Dfile.encoding=UTF-8",
                    "-Dstdout.encoding=UTF-8",
                    "-Dstderr.encoding=UTF-8",
                    "-cp",
                    System.getProperty("surefire.test.class.path", System.getProperty("java.class.path")),
                    "edu.seu.vcampus.server.ServerMain",
                    "0",
                    absoluteDatabasePath.toString())
                    .redirectErrorStream(true)
                    .redirectOutput(absoluteLogPath.toFile())
                    .start();
        } catch (IOException exception) {
            throw startupFailure(absoluteLogPath, exception);
        }

        try {
            long deadline = System.nanoTime() + START_TIMEOUT.toNanos();
            while (System.nanoTime() < deadline) {
                Matcher started = STARTED_PORT.matcher(readLog(absoluteLogPath));
                if (started.find() && process.isAlive()) {
                    return new HospitalServerProcess(process, Integer.parseInt(started.group(1)));
                }
                if (!process.isAlive()) {
                    throw startupFailure(absoluteLogPath, null);
                }
                Thread.sleep(POLL_MILLIS);
            }
            throw startupFailure(absoluteLogPath, null);
        } catch (Exception exception) {
            try {
                stop(process);
            } catch (Exception stopFailure) {
                exception.addSuppressed(stopFailure);
            }
            throw exception;
        }
    }

    int port() {
        return port;
    }

    long pid() {
        return process.pid();
    }

    @Override
    public void close() throws Exception {
        stop(process);
    }

    private static String javaExecutable() {
        Path javaHome = Path.of(System.getProperty("java.home"));
        Path java = javaHome.resolve("bin").resolve("java.exe");
        if (Files.isRegularFile(java)) {
            return java.toString();
        }
        java = javaHome.resolve("bin").resolve("java");
        return Files.isRegularFile(java) ? java.toString() : "java";
    }

    private static void stop(Process process) throws InterruptedException, IOException {
        if (!process.isAlive()) {
            return;
        }
        try {
            process.destroy();
            if (process.waitFor(STOP_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                return;
            }
            process.destroyForcibly();
            if (!process.waitFor(STOP_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                throw new IOException("Server process did not exit after forced termination.");
            }
        } catch (InterruptedException exception) {
            process.destroyForcibly();
            throw exception;
        }
    }

    private static IOException startupFailure(Path logPath, Throwable cause) {
        return new IOException("Server failed to start. Recent startup log:\n" + logTail(logPath), cause);
    }

    private static String readLog(Path logPath) {
        try {
            return Files.exists(logPath) ? Files.readString(logPath, StandardCharsets.UTF_8) : "";
        } catch (IOException ignored) {
            return "";
        }
    }

    private static String logTail(Path logPath) {
        try {
            List<String> lines = Files.exists(logPath)
                    ? Files.readAllLines(logPath, StandardCharsets.UTF_8)
                    : List.of();
            int firstLine = Math.max(0, lines.size() - LOG_TAIL_LINES);
            String tail = String.join(System.lineSeparator(), lines.subList(firstLine, lines.size()));
            return tail.length() <= LOG_TAIL_CHARACTERS
                    ? tail
                    : tail.substring(tail.length() - LOG_TAIL_CHARACTERS);
        } catch (IOException ignored) {
            return "";
        }
    }
}
