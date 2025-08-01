package de.julianweinelt.caesar.storage.sandbox;

import de.julianweinelt.caesar.Caesar;
import de.julianweinelt.caesar.auth.User;
import de.julianweinelt.caesar.storage.Configuration;
import de.julianweinelt.caesar.storage.StorageType;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class SandBoxManager {
    private final List<SandBox> sandBoxes = new ArrayList<>();

    public static SandBoxManager getInstance() {
        return Caesar.getInstance().getSandBoxManager();
    }

    public boolean workingInSandBox(@Nullable User user) {
        if (user == null) return false;
        for (SandBox sandBox : sandBoxes) if (sandBox.getUsers().contains(user.getUuid())) return true;
        return false;
    }
    public SandBox getSandBox(User user) {
        for (SandBox sandBox : sandBoxes) if (sandBox.getUsers().contains(user.getUuid())) return sandBox;
        return null;
    }
    public SandBox getSandBox(UUID boxID) {
        for (SandBox sandBox : sandBoxes) if (sandBox.getUniqueID().equals(boxID)) return sandBox;
        return null;
    }
    public Optional<SandBox> getSandBoxOptional(UUID boxID) {
        SandBox snd = getSandBox(boxID);
        if (snd == null) return Optional.empty();
        return Optional.of(snd);
    }

    public String createSandBox() {
        SandBox sandBox = new SandBox(UUID.randomUUID(), System.currentTimeMillis());
        sandBox.create(StorageType.get(Configuration.getInstance().getDatabaseType()));
        sandBoxes.add(sandBox);
        return sandBox.getSchemaName();
    }
    public void shutdownSandBox() {
        for (SandBox sandBox : sandBoxes) {
            sandBox.getUsedStorage().disconnect();
        }
    }
    public void deleteSandBox(UUID sandbox) {
        getSandBoxOptional(sandbox).ifPresent(sndBx -> {
            sandBoxes.remove(sndBx);
            sndBx.delete();
        });
    }
}