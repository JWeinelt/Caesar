package de.julianweinelt.caesar.tabula.api.workflow;

import java.util.UUID;

public class ChainedWorkFlow extends WorkFlowBase{
    public ChainedWorkFlow() {
        super(UUID.randomUUID(), "ChainedWorkflow");
    }

    @Override
    protected boolean run(Object... params) {
        for (Object o : params) {
            if (o instanceof WorkFlowBase wf) {
                wf.run(wf.getDefaultValues());
            }
        }
        return false;
    }

    @Override
    protected boolean checkParams(Object... params) {
        for (Object o : params) {
            if (!(o instanceof WorkFlowBase)) return false;
        }
        return true;
    }
}
