package edu.seu.vcampus.client.application;

import edu.seu.vcampus.client.infrastructure.CampusClient;
import edu.seu.vcampus.common.protocol.Request;
import edu.seu.vcampus.common.protocol.Response;
import edu.seu.vcampus.common.protocol.ErrorCodes;
import edu.seu.vcampus.common.user.LoginRequest;
import edu.seu.vcampus.common.user.ChangePasswordRequest;
import edu.seu.vcampus.common.user.PasswordProof;
import edu.seu.vcampus.common.user.SessionInfo;
import edu.seu.vcampus.common.user.UserActions;

import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

/**
 * Shared client services used by all Swing business modules.
 */
public final class ClientContext {

    private final CampusClient client;
    private final CampusClient cardClient;
    private final ClientSession session = new ClientSession();
    private volatile Runnable authenticationLostHandler = () -> { };

    /**
     * Creates the shared application context using one campus server.
     *
     * @param client network client
     */
    public ClientContext(CampusClient client) {
        this(client, client);
    }

    /**
     * Creates the shared application context.
     *
     * @param client campus services client (login, shop, hospital, library)
     * @param cardClient campus-card gateway client
     */
    public ClientContext(CampusClient client, CampusClient cardClient) {
        this.client = Objects.requireNonNull(client, "client must not be null");
        this.cardClient = Objects.requireNonNull(cardClient, "cardClient must not be null");
    }

    /**
     * Logs in with a development demo account and stores a successful session.
     * The supplied password array is cleared before this method returns.
     *
     * @param username login name
     * @param password caller-owned password characters
     * @return server response
     * @throws IOException when the server cannot be reached
     */
    public Response login(String username, char[] password) throws IOException {
        try {
            LoginRequest loginRequest = new LoginRequest(
                    username,
                    PasswordProof.create(username, password));
            Response response = client.send(Request.create(UserActions.LOGIN, null, loginRequest));
            if (response.isSuccess() && response.getData() instanceof SessionInfo sessionInfo) {
                session.set(sessionInfo);
            } else {
                session.clear();
            }
            return response;
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    /**
     * Sends a request with the current session token.
     *
     * @param action public action name
     * @param data serializable request DTO, or {@code null}
     * @return server response
     * @throws IOException when the server cannot be reached
     */
    public Response send(String action, Serializable data) throws IOException {
        Response response = client.send(Request.create(action, session.tokenOrNull(), data));
        if (ErrorCodes.AUTH_REQUIRED.equals(response.getCode())
                && session.current().isPresent()) {
            session.clear();
            authenticationLostHandler.run();
        }
        return response;
    }

    /**
     * Sends a request to the campus-card TCP gateway with the current session token.
     *
     * @param action public card action name
     * @param data serializable request DTO, or {@code null}
     * @return server response
     * @throws IOException when the card gateway cannot be reached
     */
    public Response sendCard(String action, Serializable data) throws IOException {
        Response response = cardClient.send(Request.create(action, session.tokenOrNull(), data));
        if (ErrorCodes.AUTH_REQUIRED.equals(response.getCode())
                && session.current().isPresent()) {
            session.clear();
            authenticationLostHandler.run();
        }
        return response;
    }

    /**
     * Logs out the current server session and always clears the local session.
     *
     * @return server response
     * @throws IOException when the server cannot be reached
     */
    public Response logout() throws IOException {
        try {
            return send(UserActions.LOGOUT, null);
        } finally {
            session.clear();
        }
    }

    /** Changes the current account password and clears the invalidated local session. */
    public Response changePassword(char[] currentPassword, char[] newPassword)
            throws IOException {
        SessionInfo current = session.current().orElseThrow(
                () -> new IllegalStateException("No authenticated session."));
        try {
            ChangePasswordRequest request = new ChangePasswordRequest(
                    PasswordProof.create(current.getUsername(), currentPassword),
                    PasswordProof.create(current.getUsername(), newPassword));
            Response response = send(UserActions.CHANGE_PASSWORD, request);
            if (response.isSuccess()) {
                session.clear();
            }
            return response;
        } finally {
            Arrays.fill(currentPassword, '\0');
            Arrays.fill(newPassword, '\0');
        }
    }

    /** @return current authenticated session, if any */
    public Optional<SessionInfo> currentSession() {
        return session.current();
    }

    /** Registers the UI action used when the server rejects an expired session. */
    public void setAuthenticationLostHandler(Runnable handler) {
        authenticationLostHandler = Objects.requireNonNull(
                handler, "handler must not be null");
    }
}
