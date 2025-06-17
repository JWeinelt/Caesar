package de.julianweinelt.caesar.exceptions;

public class MinecraftEndpointNotFoundException extends RuntimeException {
    public MinecraftEndpointNotFoundException(String endpoint) {
        super("An endpoint to a Minecraft plugin service with name " + endpoint + " was not found.");
    }
}
