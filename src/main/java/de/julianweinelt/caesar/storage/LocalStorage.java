package de.julianweinelt.caesar.storage;

import com.google.common.reflect.TypeToken;
import de.julianweinelt.caesar.util.LoadableManager;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.units.qual.C;

import java.io.File;
import java.nio.file.NoSuchFileException;

@Slf4j
public class LocalStorage extends LoadableManager<Configuration> {
    private final File file = new File("config.json");
    /**
     * Constructs a new instance of {@code LoadableManager}.
     *
     */
    protected LocalStorage() {
        super(log, false, "");
    }

    @Override
    public void loadData() {
        log.info("Loading local storage...");
        try {
            setDataToSave(loadObject(file.getPath(), new TypeToken<Configuration>(){}.getType()));
        } catch (NoSuchFileException ignored) {
            log.info("No local storage found. Creating new one...");
            setDataToSave(new Configuration());
            saveData();
        }
    }

    @Override
    public void saveData() {
        saveObject(file);
        log.info("Local storage saved.");
    }
}
