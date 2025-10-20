package de.julianweinelt.caesar.tabula.api.workflow;

import de.julianweinelt.caesar.tabula.TabulaRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class WorkFlow {
    private static final Logger log = LoggerFactory.getLogger(WorkFlow.class);

    public static Optional<WorkFlowBase> ofName(String name) {
        for (WorkFlowBase wf : TabulaRegistry.getInstance().getWorkFlows()) {
            if (wf.getName().equals(name)) return Optional.of(wf);
        }
        log.warn("No WorkFlow found with name {}. Maybe it's a typo?", name);
        return Optional.empty();
    }
}