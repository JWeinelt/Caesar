package de.julianweinelt.caesar.endpoint.wrapper;

import java.awt.*;
import java.util.UUID;

public record TicketStatus(UUID uniqueID, String statusName, String statusDescription, Color statusColor) {}