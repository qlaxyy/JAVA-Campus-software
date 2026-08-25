package edu.seu.vcampus.client.application;

import edu.seu.vcampus.client.infrastructure.CampusClient;
import edu.seu.vcampus.common.protocol.Request;
import edu.seu.vcampus.common.protocol.Response;
import edu.seu.vcampus.common.user.LoginRequest;
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
    private final ClientSession session = new ClientSession();

    /**
     * Creates the shared application context.
     *
     * @param client network client
     */
    public ClientContext(CampusClient client) {
        this.client = Objects.requireNonNull(client, "client must not be null");
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
        return client.send(Request.create(action, session.tokenOrNull(), data));
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

    /** @return current authenticated session, if any */
    public Optional<SessionInfo> currentSession() {
        return session.current();
    }
}
