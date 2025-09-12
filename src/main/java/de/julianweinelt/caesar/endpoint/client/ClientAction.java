package de.julianweinelt.caesar.endpoint.client;

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
