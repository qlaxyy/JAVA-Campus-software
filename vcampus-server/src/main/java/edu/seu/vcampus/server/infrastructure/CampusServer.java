package edu.seu.vcampus.server.infrastructure;

import edu.seu.vcampus.common.protocol.ErrorCodes;
import edu.seu.vcampus.common.protocol.Request;
import edu.seu.vcampus.common.protocol.Response;
import edu.seu.vcampus.server.module.ServerModules;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Minimal multi-client socket server used as the common project skeleton.
 */
public final class CampusServer implements AutoCloseable {

    private static final int DEFAULT_SOCKET_TIMEOUT_MILLIS = 5_000;
    private static final int MINIMUM_WORKER_THREADS = 4;

    private final int configuredPort;
    private final ExecutorService workerPool;
    private final ActionRouter actionRouter;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final CountDownLatch stopped = new CountDownLatch(1);

    private volatile ServerSocket serverSocket;
    private volatile Thread acceptThread;

    /**
     * Creates a server with a worker count based on available processors.
     *
     * @param port listening port, or 0 to select a free port
     */
    public CampusServer(int port) {
        this(port, Math.max(MINIMUM_WORKER_THREADS, Runtime.getRuntime().availableProcessors()));
    }

    /**
     * Creates a server.
     *
     * @param port listening port, or 0 to select a free port
     * @param workerThreads fixed number of request worker threads
     */
    public CampusServer(int port, int workerThreads) {
        this(port, workerThreads, ServerModules.createRouter());
    }

    /**
     * Creates a server with an explicitly supplied action router.
     *
     * @param port listening port, or 0 to select a free port
     * @param workerThreads fixed number of request worker threads
     * @param actionRouter initialized request router
     */
    public CampusServer(int port, int workerThreads, ActionRouter actionRouter) {
        if (port < 0 || port > 65_535) {
            throw new IllegalArgumentException("port must be between 0 and 65535");
        }
        if (workerThreads < 1) {
            throw new IllegalArgumentException("workerThreads must be positive");
        }
        this.configuredPort = port;
        this.actionRouter = java.util.Objects.requireNonNull(
                actionRouter, "actionRouter must not be null");
        this.workerPool = Executors.newFixedThreadPool(
                workerThreads,
                new NamedThreadFactory("vcampus-worker-"));
    }

    /**
     * Binds the server socket and starts the accept thread.
     *
     * @throws IOException when the listening socket cannot be opened
     */
    public synchronized void start() throws IOException {
        if (running.get()) {
            throw new IllegalStateException("server is already running");
        }

        serverSocket = new ServerSocket(configuredPort);
        running.set(true);
        acceptThread = new Thread(this::acceptLoop, "vcampus-acceptor");
        acceptThread.start();
    }

    /**
     * Returns the bound port. Useful when port 0 selected a test port.
     *
     * @return actual listening port
     */
    public int getPort() {
        ServerSocket socket = serverSocket;
        if (socket == null) {
            throw new IllegalStateException("server has not been started");
        }
        return socket.getLocalPort();
    }

    /**
     * Waits until the accept loop stops.
     *
     * @throws InterruptedException when the waiting thread is interrupted
     */
    public void awaitTermination() throws InterruptedException {
        stopped.await();
    }

    private void acceptLoop() {
        try {
            while (running.get()) {
                Socket clientSocket = serverSocket.accept();
                workerPool.submit(() -> handleClient(clientSocket));
            }
        } catch (SocketException exception) {
            if (running.get()) {
                logServerError("accept failed", exception);
            }
        } catch (IOException exception) {
            if (running.get()) {
                logServerError("server stopped unexpectedly", exception);
            }
        } finally {
            stopped.countDown();
        }
    }

    private void handleClient(Socket clientSocket) {
        try (clientSocket) {
            clientSocket.setSoTimeout(DEFAULT_SOCKET_TIMEOUT_MILLIS);

            try (ObjectOutputStream output = new ObjectOutputStream(clientSocket.getOutputStream())) {
                output.flush();
                try (ObjectInputStream input = new ObjectInputStream(clientSocket.getInputStream())) {
                    Object incoming = input.readObject();
                    Response response = dispatch(incoming);
                    output.writeObject(response);
                    output.flush();
                }
            }
        } catch (EOFException | SocketTimeoutException exception) {
            logServerError("client disconnected or timed out", exception);
        } catch (ClassNotFoundException | IOException exception) {
            logServerError("failed to process client request", exception);
        } catch (RuntimeException exception) {
            logServerError("unexpected request handling failure", exception);
        }
    }

    private Response dispatch(Object incoming) {
        if (!(incoming instanceof Request request)) {
            return Response.failure(
                    "UNKNOWN",
                    ErrorCodes.COMMON_INVALID_REQUEST,
                    "Request format is invalid.");
        }

        return actionRouter.dispatch(request);
    }

    private void logServerError(String summary, Exception exception) {
        System.err.printf("[%s] %s: %s%n",
                Thread.currentThread().getName(),
                summary,
                exception.getMessage());
    }

    /**
     * Stops accepting connections and shuts down the worker pool.
     */
    @Override
    public synchronized void close() {
        if (!running.getAndSet(false)) {
            return;
        }

        ServerSocket socket = serverSocket;
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException exception) {
                logServerError("failed to close server socket", exception);
            }
        }

        workerPool.shutdown();
        try {
            if (!workerPool.awaitTermination(2, TimeUnit.SECONDS)) {
                workerPool.shutdownNow();
            }
        } catch (InterruptedException exception) {
            workerPool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

}
