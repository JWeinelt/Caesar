package de.julianweinelt.caesar.endpoint.chat;

import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ChatManager {
    private static final Logger log = LoggerFactory.getLogger(ChatManager.class);

    @Getter
    @Setter
    private List<Chat> chats = new ArrayList<>();

    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private final ChatDataManager dataManager;

    private final ChatServer server;

    public ChatManager(ChatServer server) {
        this.server = server;
        dataManager = new ChatDataManager(this);
        dataManager.loadData();
        log.info("Starting chat save task...");
        scheduler.scheduleAtFixedRate(dataManager::saveData, 20, 60, TimeUnit.SECONDS);
    }

    public Chat getChat(UUID uuid) {
        for (Chat c : chats) if (c.getUniqueID().equals(uuid)) return c;
        return null;
    }

    public List<Chat> getChatsUser(UUID uuid) {
        List<Chat> result = new ArrayList<>();
        for (Chat c : chats) if (c.getUsers().contains(uuid)) result.add(c);
        return result;
    }

    public void terminate() {
        log.info("Stopping chat save scheduled executor...");
        try {
            boolean result = scheduler.awaitTermination(3, TimeUnit.SECONDS);
            if (!result) {
                log.warn("Could not terminate scheduler after 3 seconds.");
                scheduler.shutdownNow();
            } else log.info("Chat saving task has been stopped.");
        } catch (InterruptedException e) {
            log.error(e.getMessage());
        }
        log.info("Saving chats manually before shutdown...");
        dataManager.saveData();
        log.info("Done!");
    }

    public Chat createChat(UUID creator) {
        Chat chat = new Chat(server, UUID.randomUUID());
        chat.setCustomName("New Chat");
        chat.addUser(creator);
        chats.add(chat);
        return chat;
    }
}