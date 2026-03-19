package com.techgamr.mcapi.server;

import io.javalin.security.RouteRole;

public enum Role implements RouteRole {
    /**
     * An endpoint which is open to anyone.
     */
    OPEN,
    /**
     * A Minecraft user which is logged in to the web interface.
     */
    USER_LOGGED_IN,
    /**
     * A proxy's own code not tied to a user specifically.
     */
    AUTHORISED_PROXY
}
