CREATE TABLE IF NOT EXISTS punishments (
    RecordID VARCHAR(36) NOT NULL PRIMARY KEY,
    PunishmentType VARCHAR(36) NOT NULL,
    CreationDate long NOT NULL DEFAULT UNIX_TIMESTAMP(),
    CreateUserType varchar(36) NOT NULL,
    CreatedBy varchar(36) NOT NULL,
    ActionUntil long NULL,
    Reason varchar(500) NULL DEFAULT 'Not provided'
);

CREATE TABLE IF NOT EXISTS punishment_types (
    TypeID varchar(36) NOT NULL PRIMARY KEY,
    NameKey varchar(100) NOT NULL DEFAULT '',
    Name varchar(100) NOT NULL,
    TimedPossible TINYINT
);

CREATE TABLE IF NOT EXISTS user_types (
    RecordID varchar(36) NOT NULL PRIMARY KEY,
    Name varchar(50) NOT NULL
);

CREATE TABLE IF NOT EXISTS reports (
    RecordID varchar(36) NOT NULL PRIMARY KEY,
    CreatedBy varchar(36) NOT NULL,
    Reported varchar(36) NOT NULL,
    ReportType varchar(36) NOT NULL,
    ReportStatus varchar(36) NOT NULL,
    CreationDate long NOT NULL DEFAULT UNIX_TIMESTAMP()
);

CREATE TABLE IF NOT EXISTS reports_update_history (
    RecordID varchar(36) NOT NULL PRIMARY KEY,
    ReportID varchar(36) NOT NULL,
    UpdaterUserType varchar(36) NOT NULL,
    Updater varchar(36) NOT NULL,
    NewStatus varchar(36) NULL,
    Message varchar(200) NULL,
    UpdateTime long NOT NULL DEFAULT UNIX_TIMESTAMP()
);

CREATE TABLE IF NOT EXISTS reports_types (
    RecordID varchar(36) NOT NULL PRIMARY KEY,
    Name varchar(100) NOT NULL,
    NameKey varchar(20) NOT NULL
);

CREATE TABLE IF NOT EXISTS reports_status (
    RecordID varchar(36) NOT NULL PRIMARY KEY,
    Color varchar(15) NOT NULL DEFAULT '255:255:255:100',
    Name varchar(100) NOT NULL,
    NameKey varchar(20) NOT NULL,
    IsArchive TINYINT NOT NULL DEFAULT 0
);


ALTER TABLE punishments
    ADD CONSTRAINT punishments_types_fk
        FOREIGN KEY (PunishmentType) REFERENCES punishment_types (TypeID),
    ADD CONSTRAINT punishments_user_types_fk
        FOREIGN KEY (CreateUserType) REFERENCES user_types (RecordID);

ALTER TABLE reports
    ADD CONSTRAINT reports_types_fk
        FOREIGN KEY (ReportType) REFERENCES reports_types (RecordID),
    ADD CONSTRAINT reports_status_fk
        FOREIGN KEY (ReportStatus) REFERENCES reports_status (RecordID);

ALTER TABLE reports_update_history
    ADD CONSTRAINT reports_hist_report_fk
        FOREIGN KEY (ReportID) REFERENCES reports (RecordID),
    ADD CONSTRAINT reports_hist_user_fk
        FOREIGN KEY (UpdaterUserType) REFERENCES user_types (RecordID),
    ADD CONSTRAINT reports_hist_status_fk
        FOREIGN KEY (NewStatus) REFERENCES reports_status (RecordID);