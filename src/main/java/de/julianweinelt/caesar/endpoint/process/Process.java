package de.julianweinelt.caesar.endpoint.process;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter @Setter
public class Process {
    private UUID uuid;
    private UUID type;
    private UUID status;
    private UUID createdBy;
    private UUID referencePlayer;

    public Process(UUID uuid, UUID type, UUID status, UUID createdBy, UUID referencePlayer) {
        this.uuid = uuid;
        this.type = type;
        this.status = status;
        this.createdBy = createdBy;
        this.referencePlayer = referencePlayer;
    }
}