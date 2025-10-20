package de.julianweinelt.caesar.tabula.api.component;

import de.julianweinelt.caesar.tabula.api.ComponentType;
import de.julianweinelt.caesar.tabula.api.TabulaComponent;
import de.julianweinelt.caesar.tabula.api.workflow.WorkFlow;
import lombok.Getter;

@Getter
public class ComponentButton extends TabulaComponent {
    private final String text;
    private final String workFlowName;
    private boolean disabled;

    public ComponentButton(int x, int y, int minX, int minY, String text, String workFlow, boolean disabled) {
        super(ComponentType.BUTTON, x, y, minX, minY);
        this.text = text;
        this.workFlowName = workFlow;
        this.disabled = disabled;
        if (WorkFlow.ofName(workFlowName).isEmpty()) throw new IllegalArgumentException("Workflow does not exist");
    }

    public ComponentButton(int minY, int minX, String text, String workFlowName, boolean disabled) {
        super(minY, minX, ComponentType.BUTTON);
        this.text = text;
        this.workFlowName = workFlowName;
        this.disabled = disabled;
    }

    public void setDisabled(boolean disabled) {
        //TODO: Logic for sending update to client
        this.disabled = disabled;
    }
}