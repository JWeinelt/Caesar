package de.julianweinelt.caesar.core.util.ui;

import lombok.Getter;

@Getter
public class IntDisplayComponent extends DisplayComponent {
    private final int value;
    private final String key;
    private final boolean disabled;
    private final DisplayComponentType type;

    public IntDisplayComponent(int value, String key, boolean disabled) {
        this.value = value;
        this.key = key;
        this.disabled = disabled;
        this.type = DisplayComponentType.INTEGER;
    }
}
