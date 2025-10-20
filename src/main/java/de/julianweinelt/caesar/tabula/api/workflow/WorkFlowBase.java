package de.julianweinelt.caesar.tabula.api.workflow;

import lombok.Getter;

import java.util.UUID;

@Getter
public abstract class WorkFlowBase {
    private final UUID id;
    private final String name;

    public WorkFlowBase(UUID id, String name) {
        this.id = id;
        this.name = name;
    }

    public Object[] getDefaultValues() {return new Object[0];}
    public boolean execute(Object... params) {
        if (checkParams(params)) return run(params);
        return false;
    }

    protected abstract boolean run(Object... params);
    protected abstract boolean checkParams(Object... params);
}
