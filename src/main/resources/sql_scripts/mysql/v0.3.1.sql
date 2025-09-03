CREATE TABLE IF NOT EXISTS ticket_feedback (
    TicketID VARCHAR(36) PRIMARY KEY,
    FeedbackText TEXT
);

ALTER TABLE ticket_feedback
    ADD CONSTRAINT ticket_feedback_fk
        FOREIGN KEY (TicketID) REFERENCES tickets(UUID);

ALTER TABLE ticket_feedback
    ADD COLUMN Rating INT CHECK (Rating >= 1 AND Rating <= 5);