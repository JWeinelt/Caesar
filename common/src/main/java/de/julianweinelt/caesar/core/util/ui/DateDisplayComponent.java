package de.julianweinelt.caesar.core.util.ui;

import lombok.Getter;

import java.util.Date;

@Getter
public class DateDisplayComponent extends DisplayComponent {
    private final Date value;
    private final String key;
    private final boolean disabled;
    private final DisplayComponentType type;

    public DateDisplayComponent(Date value, String key, boolean disabled) {
        this.value = value;
        this.key = key;
        this.disabled = disabled;
        this.type = DisplayComponentType.DATE;
    }
}
