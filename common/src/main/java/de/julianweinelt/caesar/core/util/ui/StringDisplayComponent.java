package de.julianweinelt.caesar.core.util.ui;

import lombok.Getter;

@Getter
public class StringDisplayComponent extends DisplayComponent {
    private final String text;
    private final String key;
    private final boolean disabled;
    private final DisplayComponentType type;

    public StringDisplayComponent(String text, String key, boolean disabled) {
        this.text = text;
        this.key = key;
        this.disabled = disabled;
        this.type = DisplayComponentType.STRING;
    }
}
