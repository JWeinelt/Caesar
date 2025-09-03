package de.julianweinelt.caesar.discord.ticket.transcript;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Optional;

@Slf4j
public class TranscriptManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static void saveTranscript(TicketTranscript transcript) {
        File file = new File("data/transcripts/" + transcript.getTicketID() + ".json");
        if (file.getParentFile().mkdirs()) log.info("Created directories for ticket transcripts");
        try (FileWriter fileWriter = new FileWriter(file)) {
            fileWriter.write(GSON.toJson(transcript));
        } catch (IOException e) {
            log.error("Could not save ticket transcript for ticket {}", transcript.getTicketID(), e);
        }
    }

    public static Optional<TicketTranscript> loadTranscript(String ticketID) {
        File file = new File("data/transcripts/" + ticketID + ".json");
        if (!file.exists()) {
            log.warn("Transcript file for ticket {} does not exist", ticketID);
            return Optional.empty();
        }
        try {
            return Optional.of(GSON.fromJson(new java.io.FileReader(file), TicketTranscript.class));
        } catch (IOException e) {
            log.error("Could not load ticket transcript for ticket {}", ticketID, e);
            return Optional.empty();
        }
    }
}
