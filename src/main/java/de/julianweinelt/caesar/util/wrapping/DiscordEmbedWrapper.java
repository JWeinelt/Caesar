package de.julianweinelt.caesar.util.wrapping;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class DiscordEmbedWrapper {
    private String title;
    private String description;
    private String url;
    private String color;
    private String footer;
    private String image;
    private String thumbnail;
    private String timestamp;
    private String author;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public DiscordEmbedWrapper() {}

    public DiscordEmbedWrapper(String title, String description, String url, String color,
                               String footer, String image, String thumbnail, String timestamp, String author) {
        this.title = title;
        this.description = description;
        this.url = url;
        this.color = color;
        this.footer = footer;
        this.image = image;
        this.thumbnail = thumbnail;
        this.timestamp = timestamp;
        this.author = author;
    }

    public String toJson() {
        return GSON.toJson(this);
    }

    public static DiscordEmbedWrapper fromJson(String json) {
        return GSON.fromJson(json, DiscordEmbedWrapper.class);
    }
}