package de.julianweinelt.caesar.exceptions.logging;

import com.google.gson.GsonBuilder;
import de.julianweinelt.caesar.storage.Configuration;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Getter
public class ConfigLog {
    private String changedBy;
    private String changedAt;

    private final List<ConfigChange> changes = new ArrayList<>();

    public void logConfigChange(String changedBy, String key, Object newValue) {
        this.changedBy = changedBy;
        changedAt = Date.from(Instant.now()).toString();

        ConfigChange configChange = new ConfigChange();
        configChange.setKey(key);
        configChange.setValueOld(Configuration.getInstance().get(key));
        configChange.setValueNew(newValue);
        changes.add(configChange);
    }

    @Setter
    @Getter
    public static class ConfigChange {
        private String key;
        private Object valueOld;
        private Object valueNew;
    }

    @Override
    public String toString() {
        return new GsonBuilder().setPrettyPrinting().create().toJson(this);
    }
}