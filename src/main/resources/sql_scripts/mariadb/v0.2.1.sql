CREATE TABLE IF NOT EXISTS report_watchers (
    ReportID varchar(36) NOT NULL,
    PlayerID varchar(36) NOT NULL,
    PRIMARY KEY (ReportID, PlayerID)
);