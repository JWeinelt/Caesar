package de.julianweinelt.caesar.tabula.api.workflow;

import java.util.UUID;

public class WFBanUserDC extends WorkFlowBase{
    public WFBanUserDC() {
        super(UUID.randomUUID(), "BanUserDiscord");
    }

    @Override
    protected boolean run(Object... params) {
        String banned = (String) params[0];
        String reason = (String) params[1];
        String bannedBy = (String) params[2];
        return false;
    }

    @Override
    protected boolean checkParams(Object... params) {
        if (params.length != 3) return false;
        return params[0] instanceof String && params[1] instanceof String && params[2] instanceof String;
    }
}
