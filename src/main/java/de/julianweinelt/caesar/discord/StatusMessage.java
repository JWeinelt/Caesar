package de.julianweinelt.caesar.discord;

import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;

import java.util.UUID;

public record StatusMessage(UUID uuid, String message, OnlineStatus status, Activity.ActivityType type, int interval) {}