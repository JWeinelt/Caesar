package de.julianweinelt.caesar.core.configuration;

import de.julianweinelt.caesar.core.util.ui.DisplayComponent;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class Configuration {
    private List<DisplayComponent> components = new ArrayList<>();
    private String versionID;
    private String configVersion;

    public void addComponent(DisplayComponent component) {
        components.add(component);
    }
}
