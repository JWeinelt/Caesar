package de.julianweinelt.caesar.tabula.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public abstract class TabulaComponent {
    private final UUID uniqueID = UUID.randomUUID();

    private final ComponentType type;
    private int x;
    private int y;
    private int minX;
    private int minY;

    public TabulaComponent(ComponentType type, int x, int y, int minX, int minY) {
        this.type = type;
        this.x = x;
        this.y = y;
        this.minX = minX;
        this.minY = minY;
    }

    public TabulaComponent(int minY, int minX, ComponentType type) {
        this.minY = minY;
        this.minX = minX;
        this.type = type;
    }

    @Override
    public String toString() {
        return new GsonBuilder().setPrettyPrinting().create().toJson(this);
    }
}