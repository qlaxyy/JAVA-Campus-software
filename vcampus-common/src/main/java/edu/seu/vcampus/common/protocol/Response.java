package edu.seu.vcampus.common.protocol;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/**
 * A response returned by the server for one request.
 */
public final class Response implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String requestId;
    private final boolean success;
    private final String code;
    private final String message;
    private final Serializable data;

    /**
     * Creates an immutable response.
     *
     * @param requestId identifier of the matching request
     * @param success whether the request succeeded
     * @param code public success or error code
     * @param message safe message suitable for the client
     * @param data serializable response DTO, or {@code null}
     */
    public Response(
            String requestId,
            boolean success,
            String code,
            String message,
            Serializable data) {
        this.requestId = Objects.requireNonNull(requestId, "requestId must not be null");
        this.success = success;
        this.code = Objects.requireNonNull(code, "code must not be null");
        this.message = Objects.requireNonNull(message, "message must not be null");
        this.data = data;
    }

    /**
     * Creates a successful response for a request.
     *
     * @param request matching request
     * @param message safe result message
     * @param data serializable response DTO, or {@code null}
     * @return successful response
     */
    public static Response success(Request request, String message, Serializable data) {
        return new Response(
                request.getRequestId(),
                true,
                ErrorCodes.SUCCESS,
                message,
                data);
    }

    /**
     * Creates a failed response.
     *
     * @param requestId identifier of the matching request
     * @param code public error code
     * @param message safe error message
     * @return failed response
     */
    public static Response failure(String requestId, String code, String message) {
        return new Response(requestId, false, code, message, null);
    }

    /** @return identifier of the matching request */
    public String getRequestId() {
        return requestId;
    }

    /** @return {@code true} when the request succeeded */
    public boolean isSuccess() {
        return success;
    }

    /** @return public success or error code */
    public String getCode() {
        return code;
    }

    /** @return safe result message */
    public String getMessage() {
        return message;
    }

    /** @return serializable response DTO, or {@code null} */
    public Serializable getData() {
        return data;
    }
}
