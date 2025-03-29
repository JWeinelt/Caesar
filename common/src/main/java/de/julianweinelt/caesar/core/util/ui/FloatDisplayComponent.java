package de.julianweinelt.caesar.core.util.ui;

import lombok.Getter;

@Getter
public class FloatDisplayComponent extends DisplayComponent {
    private final float value;
    private final String key;
    private final boolean disabled;
    private final DisplayComponentType type;

    public FloatDisplayComponent(float value, String key, boolean disabled) {
        this.value = value;
        this.key = key;
        this.disabled = disabled;
        this.type = DisplayComponentType.FLOAT;
    }
}
