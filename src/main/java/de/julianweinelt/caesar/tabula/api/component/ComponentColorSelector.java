package de.julianweinelt.caesar.tabula.api.component;

import de.julianweinelt.caesar.tabula.api.ComponentType;
import de.julianweinelt.caesar.tabula.api.TabulaComponent;

public class ComponentColorSelector extends TabulaComponent {
    private final String initialColor;

    public ComponentColorSelector(int x, int y, int minX, int minY, String initialColor) {
        super(ComponentType.COLOR, x, y, minX, minY);
        this.initialColor = initialColor;
    }

    public ComponentColorSelector(int minY, int minX, String initialColor) {
        super(minY, minX, ComponentType.COLOR);
        this.initialColor = initialColor;
    }
}
