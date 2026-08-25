package edu.seu.vcampus.server.infrastructure;

import edu.seu.vcampus.common.protocol.Request;
import edu.seu.vcampus.common.protocol.Response;

/**
 * Handles one public request action on the server.
 */
@FunctionalInterface
public interface RequestHandler {

    /**
     * Handles a validated request.
     *
     * @param request incoming request
     * @return safe response for the client
     */
    Response handle(Request request);
}
