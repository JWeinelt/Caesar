package de.julianweinelt.caesar.core.util.ui;

import java.text.ParseException;
import java.text.SimpleDateFormat;

public class DisplayComponent {
    // Placeholder Class

    public static class Builder {
        public static DisplayComponent create(DisplayComponentType type, String key, Object value, boolean disabled) {
            switch (type) {
                case STRING -> {
                    return new StringDisplayComponent(value.toString(), key, disabled);
                }
                case INTEGER -> {
                    return new IntDisplayComponent(Integer.parseInt(value.toString()), key, disabled);
                }
                case FLOAT -> {
                    return new FloatDisplayComponent(Float.parseFloat(value.toString()), key, disabled);
                }
                case BOOLEAN -> {
                    return new BooleanDisplayComponent(Boolean.parseBoolean(value.toString()), key, disabled);
                }
                case DATE -> {
                    try {
                        return new DateDisplayComponent(new SimpleDateFormat("yyyy-MM-dd").parse(value.toString()), key, disabled);
                    } catch (ParseException e) {
                        return null;
                    }
                }
                case SECTION -> {
                    return new DisplayComponentSection(key, key);
                }
            }
            return null;
        }
    }
}
