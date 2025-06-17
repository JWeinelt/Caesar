package de.julianweinelt.caesar.util.wrapping;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.julianweinelt.caesar.util.DatabaseColorParser;
import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.api.EmbedBuilder;

import java.time.temporal.TemporalAccessor;

@Setter
@Getter
public class DiscordEmbedWrapper {
    private final String embedID;
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

    public DiscordEmbedWrapper(String embedID) {
        this.embedID = embedID;
    }

    public DiscordEmbedWrapper(String embedID, String title, String description, String url, String color,
                               String footer, String image, String thumbnail, String timestamp, String author) {
        this.embedID = embedID;
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

    public EmbedBuilder toEmbed() {
        return new EmbedBuilder()
                .setTitle(this.title)
                .setDescription(this.description)
                .setUrl(this.url)
                .setColor(DatabaseColorParser.parseColor(this.color))
                .setFooter(this.footer)
                .setImage(this.image)
                .setThumbnail(this.thumbnail)
                .setAuthor(this.author);
    }
}