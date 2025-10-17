package de.julianweinelt.caesar.endpoint.client;

/**
 * Defines the various actions that can be performed by the client in the Caesar endpoint.
 */
public enum ClientAction {
    AUTHENTICATE,
    HANDSHAKE,
    NOTIFICATION,
    DC_WAITING_ROOM_UPDATE,
    DC_VOICE_UPDATE,
    DC_LINK_REQUEST,
    DC_LINK_REQUEST_CODE,
    DC_LINK_SUCCESS,
    UNKNOWN;
}
