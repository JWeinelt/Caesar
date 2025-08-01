
-- Create procedure for copying schemas (sandbox mode)
DELIMITER $$

CREATE PROCEDURE create_sandbox(IN from_db VARCHAR(64), IN to_db VARCHAR(64))
BEGIN
    DECLARE done INT DEFAULT 0;
    DECLARE tbl VARCHAR(64);
    DECLARE cur CURSOR FOR
        SELECT table_name
        FROM information_schema.tables
        WHERE table_schema = from_db;

    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = 1;

    -- Create new database
    SET @sql = CONCAT('CREATE DATABASE IF NOT EXISTS ', to_db, ' COMMENT = `caesar_sandbox`');
    PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

    OPEN cur;
    read_loop: LOOP
        FETCH cur INTO tbl;
        IF done THEN
            LEAVE read_loop;
        END IF;

        -- Create tables
        SET @sql = CONCAT('CREATE TABLE ', to_db, '.', tbl,
                          ' LIKE ', from_db, '.', tbl);
        PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;


        -- Insert data
        SET @sql = CONCAT('INSERT INTO ', to_db, '.', tbl,
                          ' SELECT * FROM ', from_db, '.', tbl);
        PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

    END LOOP;
    CLOSE cur;
END$$

DELIMITER ;

-- Drop sandbox database
DELIMITER $$

CREATE PROCEDURE delete_sandbox(IN sandbox_db VARCHAR(64))
BEGIN
    DECLARE db_comment VARCHAR(256);

    -- Kommentar abrufen
    SELECT SCHEMA_COMMENT
    INTO db_comment
    FROM information_schema.SCHEMATA
    WHERE SCHEMA_NAME = sandbox_db;

    -- Prüfen, ob DB existiert
    IF db_comment IS NULL THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Sandbox database does not exist';
    END IF;

    -- Prüfen, ob Kommentar passt
    IF db_comment != 'caesar-sandbox' THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Unable to drop database: schema is not a Caesar sandbox';
    END IF;

    -- DB löschen
    SET @sql = CONCAT('DROP DATABASE ', sandbox_db);
    PREPARE stmt FROM @sql;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;

END$$

DELIMITER ;
