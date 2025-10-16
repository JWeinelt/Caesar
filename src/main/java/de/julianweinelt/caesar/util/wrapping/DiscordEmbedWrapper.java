package de.julianweinelt.caesar.util.wrapping;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.julianweinelt.caesar.util.DatabaseColorParser;
import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.util.ArrayList;
import java.util.List;

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

    private final List<FieldWrapper> fields = new ArrayList<>();

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

    public void addField(String name, String value, boolean inline) {
        this.fields.add(new FieldWrapper(name, value, inline));
    }
    public void addField(FieldWrapper field) {
        this.fields.add(field);
    }
    public void addField(MessageEmbed.Field field) {
        this.fields.add(FieldWrapper.fromField(field));
    }

    public String toJson() {
        return GSON.toJson(this);
    }

    public EmbedBuilder toEmbed() {
        EmbedBuilder eb = new EmbedBuilder();
        eb
                .setTitle(this.title)
                .setDescription(this.description)
                .setUrl(this.url)
                .setColor(DatabaseColorParser.parseColor(this.color))
                .setFooter(this.footer)
                .setImage(this.image)
                .setThumbnail(this.thumbnail)
                .setAuthor(this.author);
        for (FieldWrapper w : fields) eb.addField(w.toField());
        return eb;
    }

    public record FieldWrapper(String name, String value, boolean inline) {
        public MessageEmbed.Field toField() {
                return new MessageEmbed.Field(name, value, inline);
        }
        public static FieldWrapper fromField(MessageEmbed.Field field) {
            return new FieldWrapper(field.getName(), field.getValue(), field.isInline());
        }
    }

    public static DiscordEmbedWrapper fromJson(String json) {
        return GSON.fromJson(json, DiscordEmbedWrapper.class);
    }
}