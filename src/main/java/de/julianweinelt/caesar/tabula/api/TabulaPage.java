package de.julianweinelt.caesar.tabula.api;

import lombok.Getter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

@Getter
public class TabulaPage {
    private String name;
    private final UUID uniqueID;

    private final HashMap<String, List<TabulaComponent>> parts = new HashMap<>();


    public TabulaPage(String name) {
        this.name = name;
        this.uniqueID = UUID.randomUUID();
    }

    public TabulaPartBuilder addPart(String name, TabulaPage page) {
        return new TabulaPartBuilder(name, page);
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