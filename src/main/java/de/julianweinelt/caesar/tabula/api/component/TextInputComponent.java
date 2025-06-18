package de.julianweinelt.caesar.tabula.api.component;

import de.julianweinelt.caesar.tabula.api.TabulaComponent;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TextInputComponent extends TabulaComponent {
    private String text;
    private String placeholder;
    private int maxLetters;

    public TextInputComponent(int x, int y, int minX, int minY, String text, String placeholder, int maxLetters) {
        super(x, y, minX, minY);
        this.text = text;
        this.placeholder = placeholder;
        this.maxLetters = maxLetters;
    }
}
