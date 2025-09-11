CREATE TABLE discord_user_mappings (
    UserID varchar(36) NOT NULL,
    DiscordID varchar(20) NOT NULL
);

ALTER TABLE discord_user_mappings
    ADD CONSTRAINT dc_user_fk
        FOREIGN KEY (UserID) REFERENCES users (UUID);

ALTER TABLE punishments
    ADD COLUMN MarkDeleted TINYINT NOT NULL DEFAULT 0