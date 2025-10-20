package de.julianweinelt.caesar.tabula.api.component;

import de.julianweinelt.caesar.tabula.api.ComponentType;
import de.julianweinelt.caesar.tabula.api.TabulaComponent;
import lombok.Getter;

@Getter
public class ComponentCheckBox extends TabulaComponent {
    private final String text;
    private final String propertyVal;
    private final boolean initialState;

    public ComponentCheckBox(int minY, int minX, String text, String propertyVal, boolean initialState) {
        super(minY, minX, ComponentType.CHECKBOX);
        this.text = text;
        this.propertyVal = propertyVal;
        this.initialState = initialState;
    }
}
