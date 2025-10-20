package de.julianweinelt.caesar.action.predefined;

import de.julianweinelt.caesar.action.Action;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.JDA;

/**
 * @deprecated in favor of WorkFlowAPI
 */
@Deprecated(since = "0.2.1")
public class DiscordPlayerBlockWordAction implements Action {
    @Override
    public void run(Object... params) {
        if (params.length != 4) {
            throw new IllegalArgumentException("Not enough parameters for DiscordPlayerBlockWordAction.");
        }

        JDA jda = (JDA) params[0];
        Member member = (Member) params[1];
        String memberID = (String) params[2];
        String word = (String) params[3];

        // Logik für das Blockieren eines Wortes
        System.out.println("Das blockierte Wort wurde von " + member.getEffectiveName() + " verwendet: " + word);

        // Hier kann man die weiteren Discord-Aktionen einfügen, z.B. das Entfernen der Nachricht.
        // jda.getTextChannelById(channelId).deleteMessageById(messageId).queue();
    }
}
