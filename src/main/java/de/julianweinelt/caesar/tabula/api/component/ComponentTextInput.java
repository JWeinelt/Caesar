package de.julianweinelt.caesar.tabula.api.component;

import de.julianweinelt.caesar.tabula.api.ComponentType;
import de.julianweinelt.caesar.tabula.api.TabulaComponent;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class ComponentTextInput extends TabulaComponent {
    private String text;
    private String placeholder;
    private int maxLetters;
    private boolean useValidation = false;
    private String validationRegex = "";

    public ComponentTextInput(int x, int y, int minX, int minY, String text, String placeholder, int maxLetters) {
        super(ComponentType.TEXT_INPUT, x, y, minX, minY);
        this.text = text;
        this.placeholder = placeholder;
        this.maxLetters = maxLetters;
    }

    public ComponentTextInput(int x, int y, int minX, int minY, String text, String placeholder, int maxLetters,
                              String validationRegex) {
        super(ComponentType.TEXT_INPUT, x, y, minX, minY);
        this.text = text;
        this.placeholder = placeholder;
        this.maxLetters = maxLetters;
        this.useValidation = true;
        this.validationRegex = validationRegex;
    }
}