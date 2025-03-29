package de.julianweinelt.caesar.core.util.ui;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class DisplayComponentSection extends DisplayComponent {
    private final String key;
    private final String title;

    public DisplayComponentSection(String key, String title) {
        this.key = key;
        this.title = title;
    }

    private List<DisplayComponent> components = new ArrayList<>();


    public DisplayComponentSection addComponent(DisplayComponent component) {
        components.add(component);
        return this;
    }
}