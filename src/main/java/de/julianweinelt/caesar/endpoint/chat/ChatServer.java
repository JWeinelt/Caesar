package de.julianweinelt.caesar.endpoint.chat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.julianweinelt.caesar.ai.AIManager;
import de.julianweinelt.caesar.auth.User;
import de.julianweinelt.caesar.auth.UserManager;
import de.julianweinelt.caesar.storage.LocalStorage;
import org.java_websocket.WebSocket;
import org.java_websocket.framing.CloseFrame;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatServer extends WebSocketServer {
    private static final Logger log = LoggerFactory.getLogger(ChatServer.class);

    private final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Pattern mentionPattern = Pattern.compile("@([A-Za-z0-9_]+)");

    private final HashMap<UUID, WebSocket> connections = new HashMap<>();
    private final ChatManager chatManager;


    public ChatServer(ChatManager chatManager) {
        super(new InetSocketAddress(LocalStorage.getInstance().getData().getChatServerPort()));
        this.chatManager = chatManager;
    }

    @Override
    public void onOpen(WebSocket webSocket, ClientHandshake clientHandshake) {
        log.debug("Received handshake by client: {}", webSocket.getRemoteSocketAddress());
    }

    @Override
    public void onClose(WebSocket webSocket, int code, String information, boolean byRemote) {
        boolean isClient = getByConnection(webSocket) != null;
        log.debug("A running connection has been closed by {} with code {} and additional information {}. {}",
                (byRemote) ? "remote" : "client", code, information,
                (isClient ? "The connected WebSocket was a connected user. ID: " + getByConnection(webSocket) : ""));
    }

    @Override
    public void onMessage(WebSocket webSocket, String s) {
        // Unused, as Godot works with ByteBuffer
    }

    @Override
    public void onMessage(WebSocket conn, ByteBuffer message) {
        if (message.hasArray()) {
            String msg = new String(message.array(), StandardCharsets.UTF_8);
            JsonObject rootOBJ = JsonParser.parseString(msg).getAsJsonObject();
            ChatAction action = parseAction(rootOBJ.get("type").getAsString());
            if (ChatAction.isServerOnly(action)) {
                sendError("This action is only allowed from server side.", conn);
                return;
            }
            if (action.equals(ChatAction.UNKNOWN)) {
                sendError("Unknown action", conn);
                return;
            }
            switch (action) {
                case AUTHENTICATE -> {
                    UUID userID = UUID.fromString(rootOBJ.get("myID").getAsString());
                    connections.put(userID, conn);
                    sendHandshake(conn);
                    sendChatList(userID);
                    log.debug("Received CaesarHandshake by {} from {}", userID, conn.getRemoteSocketAddress());
                }
                case SEND_MESSAGE -> {
                    sendMessageBy(
                            UUID.fromString(rootOBJ.get("sender")
                                .getAsJsonObject().get("uuid").getAsString()),
                            rootOBJ.get("message").getAsString(),
                        UUID.fromString(rootOBJ.get("chat").getAsString()));
                }
                case LEAVE -> {
                    UUID user = UUID.fromString(rootOBJ.get("user").getAsString());
                    UUID chat = UUID.fromString(rootOBJ.get("chat").getAsString());

                    Chat ch = chatManager.getChat(chat);
                    if (ch == null) return;
                    ch.removeUser(user);
                    sendMessageSystem("{!user:" + user + "} left the chat.", chat);
                }
                case SEND_CHAT_LIST -> {
                    UUID user = UUID.fromString(rootOBJ.get("user").getAsString());
                    sendChatList(user);
                }
                case CREATE_CHAT -> {
                    UUID user = getByConnection(conn);
                    User userObj = UserManager.getInstance().getUser(user);
                    Chat chat = chatManager.createChat(user);
                    sendChatList(user);
                    sendMessageSystem("The chat \"New Chat\" has been created by " + userObj.getUsername() + ".", chat.getUniqueID());
                }
                case RENAME_CHAT -> {
                    UUID user = getByConnection(conn);
                    User userObj = UserManager.getInstance().getUser(user);
                    Chat chat = chatManager.getChat(UUID.fromString(rootOBJ.get("chat").getAsString()));
                    String newName = rootOBJ.get("newName").getAsString();
                    chat.setCustomName(newName);
                    sendChatRename(chat);
                    sendMessageSystem("[b]" + userObj.getUsername() + "[/b] set the name of the chat to [b]"
                            + newName + ".[/b]", chat.getUniqueID());
                }
                case ADD_USER -> {
                    User senderU = UserManager.getInstance().getUser(getByConnection(conn));
                    String addName = rootOBJ.get("toAdd").getAsString();
                    User userToAdd = UserManager.getInstance().getUser(addName);
                    UUID chat = UUID.fromString(rootOBJ.get("chat").getAsString());
                    Chat chat1 = chatManager.getChat(chat);
                    chat1.addUser(userToAdd.getUuid());
                    sendMessageSystem(senderU.getUsername() + " added " + userToAdd.getUsername() + " to the chat.", chat1.getUniqueID());
                    sendChatList(userToAdd.getUuid());
                }
                case DELETE_CHAT -> {

                }
                case CLOSE_REQUEST -> {
                    UUID user = getByConnection(conn);
                    connections.remove(user);
                    conn.close(CloseFrame.NORMAL, "Goodbye");
                }
            }
        }
    }

    @Override
    public void onError(WebSocket webSocket, Exception e) {}

    @Override
    public void onStart() {
        log.info("Chat Server has been started.");
        log.info("Listening on {}:{}.", getAddress().getHostName(), getPort());
    }

    public void sendMessageBy(UUID sender, String message, UUID chat) {
        User sendingUser = UserManager.getInstance().getUser(sender);

        List<ChatMention> mentions = getMentions(chat, sender, message);
        if (!mentions.isEmpty()) sendMessageMention(mentions);

        JsonObject o = new JsonObject();
        o.addProperty("type", ChatAction.MESSAGE.name());
        o.add("sender", createSenderOBJ(sender));
        o.addProperty("message", message);
        o.addProperty("chat", chat.toString());
        o.addProperty("timestamp", System.currentTimeMillis());
        Chat c = chatManager.getChat(chat);
        c.registerNewMessage(new Message(message, sendingUser.getUsername(), System.currentTimeMillis()));
        for (UUID uuid : c.getUsers()) {
            log.debug("Sending message to chat {} for user {} by {}", c.getUniqueID(), uuid, sender);
            WebSocket conn = getConnection(uuid);
            if (conn != null) {
                log.debug("Sent");
                conn.send(o.toString());
            }
        }

        if (!c.isDirectMessage() && LocalStorage.getInstance().getData().isUseAIChat()) { // TODO: Replace with global field for AI options
            if (ChatMention.isMentioned(chatManager.getJunoID(), mentions)) {
                sendMessageBy(chatManager.getJunoID(), AIManager.getInstance().answerMessage(message), chat);
            }
        } else if (c.isDirectMessage() && c.hasUser(chatManager.getJunoID())) {
            sendMessageBy(chatManager.getJunoID(), AIManager.getInstance().answerMessage(message), chat);
        }
    }

    public void sendMessageSystem(String message, UUID chat) {
        JsonObject o = new JsonObject();
        o.addProperty("type", ChatAction.MESSAGE.name());
        o.add("sender", createSenderOBJ("SYSTEM"));
        o.addProperty("message", message);
        o.addProperty("chat", chat.toString());
        o.addProperty("timestamp", System.currentTimeMillis());
        Chat c = chatManager.getChat(chat);
        for (UUID uuid : c.getUsers()) {
            WebSocket conn = getConnection(uuid);
            if (conn != null) {
                conn.send(o.toString());
            }
        }
        c.registerNewMessage(new Message(message, "SYSTEM", System.currentTimeMillis()));
    }

    public void sendChatRename(Chat chat) {
        for (UUID uuid : chat.getUsers()) {
            WebSocket conn = getConnection(uuid);
            if (conn != null) {
                JsonObject o = new JsonObject();
                o.addProperty("type", "RENAME_CHAT");
                o.addProperty("chat", chat.getUniqueID().toString());
                o.addProperty("newName", chat.getCustomName());
                conn.send(o.toString());
            }
        }
    }

    public void sendMessageMention(List<ChatMention> mentions) {
        for (ChatMention m : mentions) {
            JsonObject o = new JsonObject();
            o.addProperty("type", ChatAction.USER_MENTION.name());
            o.addProperty("chat", m.chat().toString());
            o.addProperty("timestamp", System.currentTimeMillis());
            o.addProperty("sender", m.sender().toString());
            WebSocket s = getConnection(m.mentioned());
            if (s != null) s.send(o.toString());
        }
    }

    public JsonObject createSenderOBJ(UUID sender) {
        JsonObject o = new JsonObject();
        o.addProperty("uuid", sender.toString());
        User user = UserManager.getInstance().getUser(sender);
        if (user == null) {
            o.addProperty("username", "Unknown");
        } else {
            o.addProperty("username", user.getUsername());
        }
        return o;
    }

    public JsonObject createSenderOBJ(String sender) {
        JsonObject o = new JsonObject();
        o.addProperty("uuid", "Unknown");
        o.addProperty("username", sender);
        return o;
    }

    public void sendChatList(UUID user) {
        List<Chat> chats = chatManager.getChatsUser(user);
        WebSocket conn = getConnection(user);
        if (conn != null) {
            JsonObject o = new JsonObject();
            o.addProperty("type", ChatAction.SEND_CHAT_LIST.name());
            o.add("chats", GSON.toJsonTree(chats));
            conn.send(o.toString());
        }

    }

    public WebSocket getConnection(UUID uuid) {
        return connections.getOrDefault(uuid, null);
    }
    public UUID getByConnection(WebSocket conn) {
        for (UUID uuid : connections.keySet()) {
            if (connections.get(uuid).equals(conn)) return uuid;
        }
        return null;
    }

    public ChatAction parseAction(String input) {
        try {
            return ChatAction.valueOf(input);
        } catch (IllegalArgumentException ignored) {
            return ChatAction.UNKNOWN;
        }
    }

    public void sendHandshake(WebSocket conn) {
        List<Chat> chats = chatManager.getChatsUser(getByConnection(conn));
        JsonObject o = new JsonObject();
        o.addProperty("type", ChatAction.HANDSHAKE.name());
        o.add("chats", GSON.toJsonTree(reduceChatList(chats)));
        conn.send(o.toString());
    }

    public void sendError(String message, WebSocket conn) {
        JsonObject o = new JsonObject();
        o.addProperty("message", message);
        o.addProperty("type", ChatAction.SEND_ERROR.name());
        conn.send(o.toString());
    }

    public List<ChatReduced> reduceChatList(List<Chat> chats) {
        List<ChatReduced> result = new ArrayList<>();
        for (Chat c : chats) result.add(new ChatReduced(c.getUniqueID(), c.getCustomName()));
        return result;
    }

    public List<ChatMention> getMentions(UUID chatId, UUID senderId, String message) {
        List<ChatMention> mentions = new ArrayList<>();

        Matcher matcher = mentionPattern.matcher(message);
        while (matcher.find()) {
            String username = matcher.group(1);
            User user = UserManager.getInstance().getUser(username);
            if (user != null) {
                UUID mentionedId = user.getUuid();
                if (mentionedId != null) {
                    mentions.add(new ChatMention(mentionedId, chatId, senderId));
                }
            }
        }

        return mentions;
    }

    public record ChatReduced(UUID uuid, String chatName) {}
}