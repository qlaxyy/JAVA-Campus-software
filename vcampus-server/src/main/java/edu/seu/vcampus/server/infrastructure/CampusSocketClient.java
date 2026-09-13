package edu.seu.vcampus.server.infrastructure;

import edu.seu.vcampus.common.protocol.Request;
import edu.seu.vcampus.common.protocol.Response;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Objects;

/**
 * Sends one request per TCP connection using the shared campus protocol.
 */
public final class CampusSocketClient {

    private static final int DEFAULT_TIMEOUT_MILLIS = 5_000;

    private final String host;
    private final int port;
    private final int timeoutMillis;

    /**
     * Creates a client with the default five-second timeout.
     *
     * @param host server host
     * @param port server port
     */
    public CampusSocketClient(String host, int port) {
        this(host, port, DEFAULT_TIMEOUT_MILLIS);
    }

    /**
     * Creates a client.
     *
     * @param host server host
     * @param port server port
     * @param timeoutMillis connection and read timeout in milliseconds
     */
    public CampusSocketClient(String host, int port, int timeoutMillis) {
        this.host = Objects.requireNonNull(host, "host must not be null");
        if (host.isBlank()) {
            throw new IllegalArgumentException("host must not be blank");
        }
        if (port < 1 || port > 65_535) {
            throw new IllegalArgumentException("port must be between 1 and 65535");
        }
        if (timeoutMillis < 1) {
            throw new IllegalArgumentException("timeoutMillis must be positive");
        }
        this.port = port;
        this.timeoutMillis = timeoutMillis;
    }

    /**
     * Sends one request over a new socket connection.
     *
     * @param request request to send
     * @return matching server response
     * @throws IOException when network or protocol validation fails
     */
    public Response send(Request request) throws IOException {
        Objects.requireNonNull(request, "request must not be null");
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), timeoutMillis);
            socket.setSoTimeout(timeoutMillis);
            try (ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream())) {
                output.flush();
                try (ObjectInputStream input = new ObjectInputStream(socket.getInputStream())) {
                    output.writeObject(request);
                    output.flush();
                    Object incoming = input.readObject();
                    if (!(incoming instanceof Response response)) {
                        throw new IOException("Server returned an invalid response type.");
                    }
                    if (!request.getRequestId().equals(response.getRequestId())) {
                        throw new IOException("Response requestId does not match the request.");
                    }
                    return response;
                }
            } catch (ClassNotFoundException exception) {
                throw new IOException("Unable to read the server response.", exception);
            }
        }
    }
}
