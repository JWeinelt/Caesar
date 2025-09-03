package de.julianweinelt.caesar.discord.ticket;

import com.google.gson.JsonObject;
import de.julianweinelt.caesar.ai.AIManager;
import de.julianweinelt.caesar.discord.ticket.transcript.TicketTranscript;
import de.julianweinelt.caesar.discord.ticket.transcript.TranscriptManager;
import de.julianweinelt.caesar.storage.StorageFactory;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.jetbrains.annotations.Nullable;

import java.awt.Color;
import java.util.List;
import java.util.HashMap;
import java.util.UUID;

public class TicketListener extends ListenerAdapter {
    private final String[] feedbackMessages = {
            "I'm sorry that you weren't satisfied with our support. " +
                    "If you want to give us more detailed feedback, please reply to this message.",
            "I'm sorry that you weren't satisfied with our support. " +
                    "If you want to give us more detailed feedback, please reply to this message.",
            "Thank you! What do you think we could do better?",
            "Thank you! We're glad you had a good experience. What could we do better?",
            "Awesome! We're happy that you had a great experience. What did you like the most?"
    };
    private final HashMap<String, UUID> privateFeedback = new HashMap<>();
    private final HashMap<String, Integer> ratings = new HashMap<>();

    @Override
    public void onStringSelectInteraction(StringSelectInteractionEvent e) {
        if (e.getMember() == null) return;
        e.deferReply(true).queue();
        TicketManager.getInstance().createTicket(e.getMember().getId(),
                TicketType.ofID(e.getSelectedOptions().get(0).getValue())).thenAccept(ticket -> {
            e.getHook().editOriginal("A new ticket has been created: <#" + ticket.getChannelID() + ">").queue();
        });
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent e) {
        if (privateFeedback.containsKey(e.getChannel().getId())) {
            e.getChannel().sendMessage("Thank you for your feedback! We appreciate it.").queue();
            StorageFactory.getInstance().getUsedStorage(null).saveTicketFeedback(
                    privateFeedback.get(e.getChannel().getId()),
                    ratings.getOrDefault(e.getChannel().getId(), 0),
                    e.getMessage().getContentRaw()
            );
            privateFeedback.remove(e.getChannel().getId());
            ratings.remove(e.getChannel().getId());
            return;
        }

        Ticket ticket = getTicketByChannelID(e.getChannel().getId());
        if (ticket == null) return;

        TranscriptManager.loadTranscript(ticket.getUniqueID().toString()).ifPresentOrElse(transcript -> {
            transcript.processMessage(e.getMessage());
            TranscriptManager.saveTranscript(transcript);
        }, () -> {
            TicketTranscript transcript = new TicketTranscript(ticket.getUniqueID());
            transcript.processMessage(e.getMessage());
            TranscriptManager.saveTranscript(transcript);
        });

        if (e.getMessage().getMentions().isMentioned(e.getJDA().getSelfUser()) && !e.getAuthor().isBot()) {
            e.getChannel().sendTyping().queue();
            JsonObject data = AIManager.getInstance().getDiscordMessageType(e.getMessage().getContentRaw());

            switch (data.get("type").getAsString().toLowerCase()) {
                case "close_ticket" -> {
                    e.getChannel().sendMessageEmbeds(new EmbedBuilder()
                            .setTitle("Ticket Closed")
                            .setDescription("This ticket has been closed by " + e.getAuthor().getAsMention() + ".\n\n" +
                                    "If you want to re-open it, just send any message in this channel.\n" +
                                    "After 14 days, this channel will be deleted automatically.")
                            .setColor(Color.RED)
                            .setFooter("Ticket processed by Caesar")
                            .build()
                    ).queue();
                    ticket.updateStatus(TicketStatus.of("CLOSED"));
                    e.getJDA().openPrivateChannelById(ticket.getCreator()).queue(txt -> {
                        txt.sendMessageEmbeds(new EmbedBuilder()
                                .setTitle("Thanks for contacting us!")
                                .setDescription("""
                                        Your ticket has been closed. If you have any further questions, feel
                                         free to answer in the channel withín 14 days, or create a new one.
                                        
                                        We want to make our support better for you and our players.
                                        Therefore, we would be very happy if you give us some feedback:\s
                                        How would you rate our support? (1-5)""")
                                .setColor(Color.YELLOW)
                                .setFooter("Ticket processed by Caesar")
                                .build()
                        )
                                .addActionRow(Button.primary("feedback;1;" + ticket.getUniqueID(), "⭐"),
                                        Button.primary("feedback;2;" + ticket.getUniqueID(), "⭐⭐"),
                                        Button.primary("feedback;3;" + ticket.getUniqueID(), "⭐⭐⭐"),
                                        Button.primary("feedback;4;" + ticket.getUniqueID(), "⭐⭐⭐⭐"),
                                        Button.primary("feedback;5;" + ticket.getUniqueID(), "⭐⭐⭐⭐⭐"))
                                .queue();
                    });
                    return;
                }
                default -> {
                    e.getChannel().sendMessage("I don't know how to handle this request. Please try again.").queue();
                    return;
                }
            }
        }

        if (ticket.getStatus() == TicketStatus.of("CLOSED")) {
            ticket.updateStatus(TicketStatus.of("OPEN"));
            e.getChannel().sendMessageEmbeds(new EmbedBuilder()
                    .setTitle("✔ Ticket Re-opened")
                    .setColor(Color.GREEN)
                    .setDescription("This ticket has been re-opened by " + e.getAuthor().getAsMention() + ".")
                    .build()
            ).queue();
        }
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent e) {
        if (e.getComponentId().startsWith("feedback")) {
            int i = e.getComponentId().split(";")[1].isEmpty() ? 0
                    : Integer.parseInt(e.getComponentId().split(";")[1]);
            UUID ticketID = UUID.fromString(e.getComponentId().split(";")[2]);
            e.deferEdit().queue();
            e.getChannel().sendMessage(feedbackMessages[i-1]).queue();
            privateFeedback.put(e.getChannel().getId(), ticketID);
            ratings.put(e.getChannelId(), i);
        }
    }

    @Override
    public void onGuildMemberRemove(GuildMemberRemoveEvent e) {
        List<Ticket> tickets = TicketManager.getInstance().getUserTickets(e.getUser().getId());
        for (Ticket ticket : tickets) {
            ticket.updateStatus(TicketStatus.of("CLOSED"));
            e.getGuild().getTextChannelById(ticket.getChannelID()).sendMessageEmbeds(new EmbedBuilder()
                    .setTitle("Ticket Closed")
                    .setDescription("""
                            This ticket has been closed automatically because the user has left the server.
                            
                            After 14 days, this channel will be deleted automatically.""")
                    .setColor(Color.RED)
                    .setFooter("Ticket processed by Caesar")
                    .build()
            ).queue();
        }
    }

    @Nullable
    private Ticket getTicketByChannelID(String channelID) {
        return TicketManager.getInstance().getTickets().stream().filter(t -> t.getChannelID().equals(channelID)).findFirst().orElse(null);
    }
}