package de.julianweinelt.caesar.discord.ticket;

import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class TicketListener extends ListenerAdapter {
    @Override
    public void onStringSelectInteraction(StringSelectInteractionEvent e) {
        if (e.getInteraction().getComponentId().equals("create_ticket")) {
            e.deferReply(true).queue();
            TicketManager.getInstance().getTicketTypes().stream()
                    .filter(t -> t.name().equals(e.getValues().get(0))).findFirst().ifPresent(type ->
                            TicketManager.getInstance().createTicket(e.getUser().getId(), type).thenAccept(ticket -> {
                                e.getHook().editOriginal("Created ticket: " + ticket.getTextChannel().getAsMention()).queue();
                            }));
        }
    }
}