CREATE TABLE punishments (
    RecordID VARCHAR(36) NOT NULL PRIMARY KEY,
    PunishmentType VARCHAR(36) NOT NULL,
    CreationDate BIGINT NOT NULL DEFAULT DATEDIFF(SECOND, '1970-01-01', GETUTCDATE()),
    CreateUserType varchar(36) NOT NULL,
    CreatedBy varchar(36) NOT NULL,
    ActionUntil BIGINT NULL,
    Reason varchar(500) NULL DEFAULT 'Not provided'
);

CREATE TABLE punishment_types (
    TypeID varchar(36) NOT NULL PRIMARY KEY,
    NameKey varchar(100) NOT NULL DEFAULT '',
    Name varchar(100) NOT NULL,
    TimedPossible TINYINT
);

CREATE TABLE user_types (
    RecordID varchar(36) NOT NULL PRIMARY KEY,
    Name varchar(50) NOT NULL
);

CREATE TABLE reports (
    RecordID varchar(36) NOT NULL PRIMARY KEY,
    CreatedBy varchar(36) NOT NULL,
    Reported varchar(36) NOT NULL,
    ReportType varchar(36) NOT NULL,
    ReportStatus varchar(36) NOT NULL,
    CreationDate BIGINT NOT NULL DEFAULT DATEDIFF(SECOND, '1970-01-01', GETUTCDATE())
);

CREATE TABLE reports_update_history (
    RecordID varchar(36) NOT NULL PRIMARY KEY,
    ReportID varchar(36) NOT NULL,
    UpdaterUserType varchar(36) NOT NULL,
    Updater varchar(36) NOT NULL,
    NewStatus varchar(36) NULL,
    Message varchar(200) NULL,
    UpdateTime BIGINT NOT NULL DEFAULT DATEDIFF(SECOND, '1970-01-01', GETUTCDATE())
);

CREATE TABLE reports_types (
    RecordID varchar(36) NOT NULL PRIMARY KEY,
    Name varchar(100) NOT NULL,
    NameKey varchar(20) NOT NULL
);

CREATE TABLE reports_status (
    RecordID varchar(36) NOT NULL PRIMARY KEY,
    Color varchar(15) NOT NULL DEFAULT '255:255:255:100',
    Name varchar(100) NOT NULL,
    NameKey varchar(20) NOT NULL,
    IsArchive TINYINT NOT NULL DEFAULT 0
);

CREATE TABLE players (
    PlayerID varchar(36) NOT NULL PRIMARY KEY,
    PlayerNumber int NOT NULL
);

CREATE TABLE process_player_assignment (
    ProcessID varchar(36) NOT NULL,
    PlayerID varchar(36) NOT NULL
);

CREATE TABLE players_notes (
    RecordID varchar(36) NOT NULL PRIMARY KEY,
    PlayerID varchar(36) NOT NULL,
    UserID varchar(36) NOT NULL,
    Note varchar(500) NOT NULL,
    CreationDate BIGINT NOT NULL DEFAULT DATEDIFF(SECOND, '1970-01-01', GETUTCDATE())
);

CREATE TABLE players_mc_accounts (
    PlayerID varchar(36) NOT NULL,
    MC_UUID varchar(36) NOT NULL,
    MC_Name varchar(36) NOT NULL
);

ALTER TABLE punishments
    ADD PlayerID VARCHAR(36) NOT NULL DEFAULT '';


ALTER TABLE players_notes
    ADD CONSTRAINT player_note_player_fk
        FOREIGN KEY (PlayerID) REFERENCES players(PlayerID);
ALTER TABLE players_notes
    ADD CONSTRAINT player_note_user_fk
        FOREIGN KEY (UserID) REFERENCES users (UUID);

ALTER TABLE players_mc_accounts
    ADD CONSTRAINT players_mc_fk
        FOREIGN KEY (PlayerID) REFERENCES players (PlayerID);

ALTER TABLE process_player_assignment
    ADD CONSTRAINT process_player_process_id_fk
        FOREIGN KEY (ProcessID) REFERENCES processes (ProcessID);
ALTER TABLE process_player_assignment
    ADD CONSTRAINT process_player_player_id_fk
        FOREIGN KEY (PlayerID) REFERENCES players(PlayerID);


ALTER TABLE punishments
    ADD CONSTRAINT punishments_types_fk
        FOREIGN KEY (PunishmentType) REFERENCES punishment_types (TypeID);
ALTER TABLE punishments
    ADD CONSTRAINT punishments_player_fk
        FOREIGN KEY (PlayerID) REFERENCES players (PlayerID);
ALTER TABLE punishments
    ADD CONSTRAINT punishments_user_types_fk
        FOREIGN KEY (CreateUserType) REFERENCES user_types (RecordID);

ALTER TABLE reports
    ADD CONSTRAINT reports_types_fk
        FOREIGN KEY (ReportType) REFERENCES reports_types (RecordID);
ALTER TABLE reports
    ADD CONSTRAINT reports_status_fk
        FOREIGN KEY (ReportStatus) REFERENCES reports_status (RecordID);

ALTER TABLE reports_update_history
    ADD CONSTRAINT reports_hist_report_fk
        FOREIGN KEY (ReportID) REFERENCES reports (RecordID);
ALTER TABLE reports_update_history
    ADD CONSTRAINT reports_hist_user_fk
        FOREIGN KEY (UpdaterUserType) REFERENCES user_types (RecordID);
ALTER TABLE reports_update_history
    ADD CONSTRAINT reports_hist_status_fk
        FOREIGN KEY (NewStatus) REFERENCES reports_status (RecordID);

INSERT INTO user_types (RecordID, Name) VALUES ('b1814b18-664e-4d4c-9a0d-2151fbc5e8ef', 'Console'),
                                               ('b845e723-a84e-4d89-b1fa-e35a415dd173', 'MinecraftPlayer'),
                                               ('cab5044e-786b-456b-aaf6-435f5ac685b5', 'CaesarUser');