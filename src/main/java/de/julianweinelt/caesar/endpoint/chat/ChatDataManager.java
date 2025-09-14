package de.julianweinelt.caesar.endpoint.chat;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import de.julianweinelt.caesar.Caesar;
import de.julianweinelt.caesar.plugin.Registry;
import de.julianweinelt.caesar.plugin.event.Event;
import de.julianweinelt.caesar.storage.Configuration;
import de.julianweinelt.caesar.storage.LocalStorage;
import de.julianweinelt.caesar.util.LoadableManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.reflect.Type;
import java.nio.file.NoSuchFileException;
import java.util.Base64;
import java.util.List;

public class ChatDataManager extends LoadableManager<List<Chat>> {
    private static final Logger log = LoggerFactory.getLogger(ChatDataManager.class);

    private final File file = new File("data", "chats.cac");

    private final ChatManager chatManager;
    /**
     * Constructs a new instance of {@code LoadableManager}.
     */
    protected ChatDataManager(ChatManager manager) {
        super(log, true,  Base64.getDecoder().decode(Configuration.getInstance().getConnectionAPISecret()));
        this.chatManager = manager;
    }

    @Override
    public void loadData() {
        if (!file.exists()) {
            log.debug("Chat data file does not exist");
            saveData();
            return;
        }
        Type t = new TypeToken<List<Chat>>(){}.getType();
        try {
            chatManager.setChats(loadObject(file.toPath().toString(), t));
        } catch (NoSuchFileException e) {
            log.error("Failed to load chat data from file '{}': {}", file.toPath().toString(), e.getMessage());
        }
    }

    @Override
    public void saveData() {
        String secret = Configuration.getInstance().getConnectionAPISecret();
        try {
            byte[] iv = Base64.getDecoder().decode(secret);
            if (iv == null || iv.length != 16) {
                String newKey = Base64.getEncoder().encodeToString(Caesar.getInstance().generateIV());
                log.info("New key: {}", newKey);
                Configuration.getInstance().setConnectionAPISecret(newKey);
                LocalStorage.getInstance().saveData();
            }
        } catch (IllegalArgumentException e) {
            String newKey = Base64.getEncoder().encodeToString(Caesar.getInstance().generateIV());
            log.info("New key: {}", newKey);
            Configuration.getInstance().setConnectionAPISecret(newKey);
            LocalStorage.getInstance().saveData();
        }
        setDataToSave(chatManager.getChats());
        saveObject(file);
        Registry.getInstance().callEvent(new Event("ChatDataSaveEvent")
                .set("data", getSaveData())
                .set("json", new Gson().toJson(getSaveData()))
                .set("manager", chatManager));
    }
}