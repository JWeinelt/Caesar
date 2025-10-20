package de.julianweinelt.caesar.tabula.api;

import com.google.gson.GsonBuilder;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class TabulaPage {
    private String name;
    private final UUID uniqueID;
    private boolean requirePermission;
    private String displayPermission;

    private final HashMap<String, List<TabulaComponent>> parts = new HashMap<>();


    public TabulaPage(String name) {
        this.name = name;
        this.uniqueID = UUID.randomUUID();
    }

    public TabulaPage(String name, UUID uniqueID, String displayPermission) {
        this.name = name;
        this.uniqueID = uniqueID;
        this.requirePermission = true;
        this.displayPermission = displayPermission;
    }

    public TabulaPartBuilder addPart(String name) {
        return new TabulaPartBuilder(name, this);
    }


    @Override
    public String toString() {
        return new GsonBuilder().setPrettyPrinting().create().toJson(this);
    }


    @Getter
    public static class TabulaPartBuilder {
        private final String name;
        private final TabulaPage page;
        private final List<TabulaComponent> components = new ArrayList<>();

        public TabulaPartBuilder(String name, TabulaPage page) {
            this.name = name;
            this.page = page;
        }

        public TabulaPartBuilder addComponent(TabulaComponent component) {
            components.add(component);
            return this;
        }

        public void build() {
            page.parts.put(name, components);
        }
    }
}