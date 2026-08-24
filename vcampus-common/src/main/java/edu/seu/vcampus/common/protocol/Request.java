package edu.seu.vcampus.common.protocol;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * A request sent from the client to the server.
 */
public final class Request implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String requestId;
    private final String action;
    private final String token;
    private final Serializable data;

    /**
     * Creates an immutable request.
     *
     * @param requestId unique identifier used to correlate logs and responses
     * @param action public action name such as {@code COMMON.PING}
     * @param token authentication token, or {@code null} before login
     * @param data serializable request DTO, or {@code null}
     */
    public Request(String requestId, String action, String token, Serializable data) {
        this.requestId = requireText(requestId, "requestId");
        this.action = requireText(action, "action");
        this.token = token;
        this.data = data;
    }

    /**
     * Creates a request with a generated request identifier.
     *
     * @param action public action name
     * @param token authentication token, or {@code null}
     * @param data serializable request DTO, or {@code null}
     * @return a new request
     */
    public static Request create(String action, String token, Serializable data) {
        return new Request(UUID.randomUUID().toString(), action, token, data);
    }

    /**
     * Creates the common connectivity-check request.
     *
     * @return a new ping request
     */
    public static Request ping() {
        return create(Actions.PING, null, null);
    }

    /** @return unique request identifier */
    public String getRequestId() {
        return requestId;
    }

    /** @return public action name */
    public String getAction() {
        return action;
    }

    /** @return authentication token, or {@code null} */
    public String getToken() {
        return token;
    }

    /** @return serializable request DTO, or {@code null} */
    public Serializable getData() {
        return data;
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
