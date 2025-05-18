package de.julianweinelt.caesar.discord;

import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;

public record StatusMessage(String message, OnlineStatus status, Activity.ActivityType type, int interval) {}