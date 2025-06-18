package de.julianweinelt.caesar.tabula.api.component;

import de.julianweinelt.caesar.tabula.api.ComponentType;
import de.julianweinelt.caesar.tabula.api.TabulaComponent;
import lombok.Getter;

@Getter
public class PanelComponent extends TabulaComponent {
    private final String part;
    private final PanelDirection direction;

    public PanelComponent(ComponentType type, int x, int y, int minX, int minY, String part, PanelDirection direction) {
        super(ComponentType.PANEL, x, y, minX, minY);
        this.part = part;
        this.direction = direction;
    }

    public enum PanelDirection {
        HORIZONTAL,
        VERTICAL
    }
}