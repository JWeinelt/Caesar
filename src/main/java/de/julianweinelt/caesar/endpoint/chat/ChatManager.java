package de.julianweinelt.caesar.endpoint.chat;

import de.julianweinelt.caesar.Caesar;
import de.julianweinelt.caesar.auth.User;
import de.julianweinelt.caesar.auth.UserManager;
import de.julianweinelt.caesar.plugin.Registry;
import de.julianweinelt.caesar.plugin.event.Event;
import de.julianweinelt.caesar.plugin.event.Subscribe;
import de.julianweinelt.caesar.storage.LocalStorage;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
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

    private ChatServer server;

    public ChatManager() {
        registerEvents();
        Registry.getInstance().registerListener(this, Registry.getInstance().getSystemPlugin());
        dataManager = new ChatDataManager(this);
        dataManager.loadData();
    }

    /**
     * Gets the singleton instance of the ChatManager.
     * @return The ChatManager instance.
     */
    public static ChatManager getInstance() {
        return Caesar.getInstance().getChatManager();
    }

    /**
     * Registers all events related to the ChatManager.
     * This method should only be called once at startup by the system.
     */
    @ApiStatus.Internal
    private void registerEvents() {
        Registry.getInstance().registerEvents(
                "ChatDataSaveEvent",
                "ChatServerStartupEvent",
                "ChatServerShutdownEvent",
                "ChatActionEvent"
        );
    }

    /**
     * Handles the UserCreateEvent to create a DM chat with Juno AI for new users if AI chat is enabled.
     * @param e The {@link Event} object containing user information.
     */
    @Subscribe("UserCreateEvent")
    public void onUserCreate(Event e) {
        UUID userID = e.get("uuid").getAs(UUID.class);
        if (LocalStorage.getInstance().getData().isUseAIChat()) {
            createDMChat(userID, junoID);
        }
    }

    /**
     * Handles the ChatServerStartupEvent to start the chat save task and check for Juno DM chats.
     * @param e The {@link Event} object containing server information.
     */
    @Subscribe("ChatServerStartupEvent")
    public void onChatServerStartup(Event e) {
        this.server = e.get("server").getAs(ChatServer.class);
        log.info("Starting chat save task...");
        checkJunoDM();
        scheduler.scheduleAtFixedRate(dataManager::saveData, 20, 60, TimeUnit.SECONDS);
        log.info("Done! Saving chats every 60 seconds.");
    }

    /**
     * Checks if every user has a DM chat with Juno AI and creates one if not.
     */
    private void checkJunoDM() {
        // Check for each user if there already is a chat instance of the user with Juno
        log.debug("Checking if every user has a DM chat with Juno...");
        if (!LocalStorage.getInstance().getData().isUseAIChat()) {
            log.debug("AI chat disabled. Skipping...");
            return;
        }
        for (User u : UserManager.getInstance().getUsers()) {
            boolean found = false;
            boolean hasMessage = false;
            Chat aiChat = null;
            for (Chat chat : chats) {
                if (chat.isDirectMessage() && chat.hasUser(u) && chat.hasUser(junoID)) {
                    found = true;
                    hasMessage = !chat.getMessages().isEmpty();
                    aiChat = chat;
                    break;
                }
            }
            if (!found) {
                aiChat = createDMChat(u.getUuid(), junoID);
            }
            if (!hasMessage) {
                aiChat.registerNewMessage(new Message("Hello! I'm Juno AI, your smart assistant.", "Juno AI",
                        System.currentTimeMillis()));
            }
        }
    }

    /**
     * Gets a chat by its UUID.
     * @param uuid The {@link UUID} of the chat.
     * @return The {@link Chat} object, or {@code null} if not found.
     */
    //TODO: Change to Optional<Chat> in future versions
    @Nullable
    public Chat getChat(UUID uuid) {
        for (Chat c : chats) if (c.getUniqueID().equals(uuid)) return c;
        return null;
    }

    /**
     * Checks if a chat exists by its UUID.
     * @param uuid The {@link UUID} of the chat.
     * @return {@code true} if the chat exists, {@code false} otherwise.
     */
    public boolean chatExists(UUID uuid) {
        return getChat(uuid) != null;
    }

    /**
     * Gets all chats a user is part of.
     * @param uuid The {@link UUID} of the user.
     * @return A {@link List} of {@link Chat} objects the user is part of.
     */
    public List<Chat> getChatsUser(UUID uuid) {
        List<Chat> result = new ArrayList<>();
        for (Chat c : chats) if (c.getUsers().contains(uuid)) result.add(c);
        return result;
    }

    /**
     * Terminates the ChatManager, stopping the scheduler and saving data.
     */
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

    /**
     * Creates a new chat with the specified creator.
     * @param creator The {@link UUID} of the chat creator.
     * @return The newly created {@link Chat} object.
     */
    public Chat createChat(UUID creator) {
        Chat chat = new Chat(server, UUID.randomUUID());
        chat.setCustomName("New Chat");
        chat.addUser(creator);
        chats.add(chat);
        return chat;
    }

    /**
     * Creates a new direct message chat between the creator and participant.
     * @param creator The {@link UUID} of the chat creator.
     * @param participant The {@link UUID} of the chat participant.
     * @return The newly created {@link Chat} object.
     */
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

    /**
     * Deletes a chat by its UUID.
     * @param uuid The {@link UUID} of the chat to delete.
     * @return {@code true} if the chat was deleted, {@code false} if it did not exist.
     */
    public boolean deleteChat(UUID uuid) {
        if (!chatExists(uuid)) return false;
        chats.remove(getChat(uuid));
        return true;
    }

    /**
     * Deletes a chat.
     * @param chat The {@link Chat} to delete.
     * @return {@code true} if the chat was deleted, {@code false} if it did not exist.
     */
    public boolean deleteChat(Chat chat) {
        return deleteChat(chat.getUniqueID());
    }
}