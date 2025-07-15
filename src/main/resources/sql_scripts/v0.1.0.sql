-- Create script base --
-- v1.0.1

CREATE TABLE IF NOT EXISTS users (
                                     UUID                VARCHAR(36) NOT NULL PRIMARY KEY,
                                     Username            VARCHAR(20) NOT NULL UNIQUE,
                                     PasswordHashed      INT NOT NULL,
                                     CreationDate        DATETIME NULL,
                                     Active              TINYINT NOT NULL DEFAULT 1,
                                     NewlyCreated        TINYINT NOT NULL DEFAULT 1,
                                     ApplyPasswordPolicy TINYINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS permissions (
                                           UUID           VARCHAR(36) NOT NULL PRIMARY KEY,
                                           NameKey        VARCHAR(60) NOT NULL UNIQUE,
                                           PermissionKey  VARCHAR(60) NOT NULL UNIQUE,
                                           DefaultGranted TINYINT NULL
);

CREATE TABLE IF NOT EXISTS user_permissions (
                                                UserID       VARCHAR(36) NOT NULL,
                                                PermissionID VARCHAR(36) NOT NULL
);

CREATE TABLE IF NOT EXISTS roles (
                                     UUID         VARCHAR(36) NOT NULL PRIMARY KEY,
                                     NameKey      VARCHAR(60) NULL,
                                     DisplayColor VARCHAR(16) NOT NULL DEFAULT '0;0;0;100',
                                     CreationDate DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS user_roles (
                                          UserID VARCHAR(36) NOT NULL,
                                          RoleID VARCHAR(36) NOT NULL
);

CREATE TABLE IF NOT EXISTS role_permissions (
                                                RoleID       VARCHAR(36) NOT NULL,
                                                PermissionID VARCHAR(36) NOT NULL
);

CREATE TABLE IF NOT EXISTS process_status_names (
                                                    UUID        VARCHAR(36) NOT NULL PRIMARY KEY,
                                                    StatusName  VARCHAR(36) NOT NULL UNIQUE,
                                                    Color       VARCHAR(16) NOT NULL DEFAULT '0;0;0;100',
                                                    Description VARCHAR(150) NULL
);

CREATE TABLE IF NOT EXISTS ticket_status_names (
                                                   UUID        VARCHAR(36) NOT NULL PRIMARY KEY,
                                                   StatusName  VARCHAR(36) NOT NULL UNIQUE,
                                                   Color       VARCHAR(16) NOT NULL DEFAULT '0;0;0;100',
                                                   Description VARCHAR(150) NULL
);

CREATE TABLE IF NOT EXISTS ticket_types (
                                            TypeID          VARCHAR(36) NOT NULL PRIMARY KEY,
                                            TypeName        VARCHAR(16) NOT NULL,
                                            Prefix          VARCHAR(15) NOT NULL DEFAULT 'ticket',
                                            ShowInSelection TINYINT NOT NULL DEFAULT 1,
                                            SelectionEmoji  NVARCHAR(16) NULL,
                                            SelectionText   VARCHAR(30) NOT NULL DEFAULT 'Ticket'
);

CREATE TABLE IF NOT EXISTS process_types (
                                             TypeID      VARCHAR(36) NOT NULL PRIMARY KEY,
                                             TypeName    VARCHAR(30) NOT NULL,
                                             Active      TINYINT NOT NULL DEFAULT 1,
                                             UsePattern  TINYINT NOT NULL DEFAULT 0,
                                             PatternUsed VARCHAR(36) NULL
);

CREATE TABLE IF NOT EXISTS processes (
                                         ProcessID    VARCHAR(36) NOT NULL PRIMARY KEY,
                                         CreatedBy    VARCHAR(36) NOT NULL,
                                         Status       VARCHAR(36) NOT NULL,
                                         ProcessType  VARCHAR(36) NOT NULL,
                                         CreationDate DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                         Comment      VARCHAR(150) NOT NULL DEFAULT 'Nothing to see here'
);

CREATE TABLE IF NOT EXISTS tickets (
                                       UUID         VARCHAR(36) NOT NULL PRIMARY KEY,
                                       CreatedBy    VARCHAR(140) NULL,
                                       HandledBy    VARCHAR(140) NULL,
                                       CreationDate DATETIME NULL,
                                       TicketStatus VARCHAR(36) NOT NULL,
                                       TicketType   VARCHAR(36) NOT NULL,
                                       ChannelID    VARCHAR(45) NOT NULL
);

CREATE TABLE IF NOT EXISTS ticket_transcripts (
                                                  TicketID       VARCHAR(36) NOT NULL,
                                                  SenderName     VARCHAR(50) NOT NULL,
                                                  MessageContent VARCHAR(5000) NULL,
                                                  SentDate       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS server_data (
                                           UUID      VARCHAR(36) NOT NULL,
                                           Name      VARCHAR(80) NOT NULL,
                                           TimeStamp DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                           Players   INT NOT NULL DEFAULT 0,
                                           cpu       FLOAT NOT NULL,
                                           memory    INT NOT NULL,
                                           TPS       INT NOT NULL DEFAULT 20
);


ALTER TABLE user_permissions
    ADD CONSTRAINT user_permissions__perm_fk
        FOREIGN KEY (PermissionID) REFERENCES permissions (UUID),
    ADD CONSTRAINT user_permissions_users_UUID_fk
        FOREIGN KEY (UserID) REFERENCES users (UUID);
ALTER TABLE user_roles
    ADD CONSTRAINT user_roles_roles_role_fk
        FOREIGN KEY (RoleID) REFERENCES roles (UUID),
    ADD CONSTRAINT user_roles_users_user_fk
        FOREIGN KEY (UserID) REFERENCES users (UUID);
ALTER TABLE role_permissions
    ADD CONSTRAINT role_permissions_permissions_id_fk
        FOREIGN KEY (PermissionID) REFERENCES permissions (UUID),
    ADD CONSTRAINT role_permissions_roles_id_fk
        FOREIGN KEY (RoleID) REFERENCES roles (UUID);
ALTER TABLE processes
    ADD CONSTRAINT processes_process_status_names_UUID_fk
        FOREIGN KEY (Status) REFERENCES process_status_names (UUID),
    ADD CONSTRAINT processes_process_types_TypeID_fk
        FOREIGN KEY (ProcessType) REFERENCES process_types (TypeID),
    ADD CONSTRAINT processes_users_UUID_fk
        FOREIGN KEY (CreatedBy) REFERENCES users (UUID);
ALTER TABLE tickets
    ADD CONSTRAINT tickets_ticket_status_names_UUID_fk
        FOREIGN KEY (TicketStatus) REFERENCES ticket_status_names (UUID),
    ADD CONSTRAINT tickets_ticket_types_UUID_fk
        FOREIGN KEY (TicketType) REFERENCES ticket_types (TypeID);
ALTER TABLE ticket_transcripts
    ADD CONSTRAINT ticket_transcripts_tickets_UUID_fk
        FOREIGN KEY (TicketID) REFERENCES tickets (UUID);