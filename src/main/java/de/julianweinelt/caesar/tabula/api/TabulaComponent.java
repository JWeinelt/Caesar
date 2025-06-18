package de.julianweinelt.caesar.tabula.api;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TabulaComponent {
    private int x;
    private int y;
    private int minX;
    private int minY;

    public TabulaComponent(int x, int y, int minX, int minY) {
        this.x = x;
        this.y = y;
        this.minX = minX;
        this.minY = minY;
    }
}