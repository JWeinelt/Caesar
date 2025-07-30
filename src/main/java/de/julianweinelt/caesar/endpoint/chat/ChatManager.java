package de.julianweinelt.caesar.endpoint.chat;

import de.julianweinelt.caesar.auth.User;
import de.julianweinelt.caesar.auth.UserManager;
import de.julianweinelt.caesar.plugin.Registry;
import de.julianweinelt.caesar.plugin.event.Event;
import de.julianweinelt.caesar.plugin.event.Subscribe;
import de.julianweinelt.caesar.storage.LocalStorage;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ChatManager {
    private final Logger log = LoggerFactory.getLogger(ChatManager.class);
    @Getter
    private final UUID junoID = UUID.fromString("438c2559-6c71-44ec-8a2c-b16304f62939");

    @Getter
    @Setter
    private List<Chat> chats = new ArrayList<>();

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private final ChatDataManager dataManager;

    private final ChatServer server;

    public ChatManager(ChatServer server) {
        registerEvents();
        Registry.getInstance().registerListener(this, Registry.getInstance().getSystemPlugin());
        this.server = server;
        dataManager = new ChatDataManager(this);
        dataManager.loadData();
        log.info("Starting chat save task...");
        checkJunoDM();
        scheduler.scheduleAtFixedRate(dataManager::saveData, 20, 60, TimeUnit.SECONDS);
    }

    private void registerEvents() {
        Registry.getInstance().registerEvents(
                "ChatDataSaveEvent",
                "ChatServerShutdownEvent",
                "ChatActionEvent"
        );
    }

    @Subscribe("UserCreateEvent")
    public void onUserCreate(Event e) {
        UUID userID = e.get("uuid").getAs(UUID.class);
        if (LocalStorage.getInstance().getData().isUseAIChat()) {
            createDMChat(userID, junoID);
        }
    }

    private void checkJunoDM() {
        // Check for each user if there already is a chat instance of the user with Juno
        log.debug("Checking if every user has a DM chat with Juno...");
        if (!LocalStorage.getInstance().getData().isUseAIChat()) {
            log.debug("AI chat disabled. Skipping...");
            return;
        }
        for (User u : UserManager.getInstance().getUsers()) {
            for (Chat chat : chats) {
                if (chat.isDirectMessage() && chat.hasUser(u) && chat.hasUser(junoID)) break;
                else {
                    createDMChat(u.getUuid(), junoID);
                }
            }
        }
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
        Registry.getInstance().callEvent(new Event("ChatServerShutdownEvent")
                .set("chatManager", this)
                .set("server", server));
    }

    public Chat createChat(UUID creator) {
        Chat chat = new Chat(server, UUID.randomUUID());
        chat.setCustomName("New Chat");
        chat.addUser(creator);
        chats.add(chat);
        return chat;
    }

    public Chat createDMChat(UUID creator, UUID participant) {
        Chat chat = new Chat(server, UUID.randomUUID());
        chat.setCustomName("<DM>"); //TODO: Set name to the username of the other participant for each user in direct chat
        chat.addUser(creator);
        chat.addUser(participant);
        chat.setPublicChat(false);
        chat.setDirectMessage(true);
        chats.add(chat);
        return chat;
    }
}