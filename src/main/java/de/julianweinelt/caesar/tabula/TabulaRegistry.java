package de.julianweinelt.caesar.tabula;

import de.julianweinelt.caesar.Caesar;
import de.julianweinelt.caesar.tabula.api.TabulaPage;
import de.julianweinelt.caesar.tabula.api.workflow.WorkFlowBase;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

@Getter
public class TabulaRegistry {
    private static final Logger log = LoggerFactory.getLogger(TabulaRegistry.class);
    private final List<TabulaPage> pages = new ArrayList<>();
    private final List<WorkFlowBase> workFlows = new ArrayList<>();

    public static TabulaRegistry getInstance() {
        return Caesar.getInstance().getTabulaRegistry();
    }

    public void registerPage(TabulaPage page) {
        pages.add(page);
        log.info("Page {} registered to Tabula.", page);
    }
}