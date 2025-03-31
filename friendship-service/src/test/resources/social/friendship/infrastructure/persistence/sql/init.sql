CREATE TABLE user (
                      id VARCHAR(255) NOT NULL PRIMARY KEY
);

CREATE TABLE friendship_request (
                                    user_to VARCHAR(255) NOT NULL,
                                    user_from VARCHAR(255) NOT NULL,
                                    FOREIGN KEY (user_to) REFERENCES user(id) ON DELETE CASCADE,
                                    FOREIGN KEY (user_from) REFERENCES user(id) ON DELETE CASCADE,
                                    PRIMARY KEY (user_to, user_from)
);

CREATE TABLE friendship (
                            user1 VARCHAR(255) NOT NULL,
                            user2 VARCHAR(255) NOT NULL,
                            FOREIGN KEY (user1) REFERENCES user(id) ON DELETE CASCADE,
                            FOREIGN KEY (user2) REFERENCES user(id) ON DELETE CASCADE,
                            PRIMARY KEY (user1, user2)
);

-- Cambia il delimitatore per distinguere le istruzioni procedurali dalle normali query SQL
DELIMITER $$

-- Crea un trigger che si attiva prima di ogni inserimento nella tabella 'friendship', in modo da garantire che user1
-- sia sempre minore di user2 per ogni riga inserita, in modo da rendere indifferente l'ordine di inserimento
CREATE TRIGGER before_insert_friendship
    BEFORE INSERT ON friendship  -- Specifica che il trigger si applica alla tabella 'friendship'
    FOR EACH ROW                 -- Indica che il trigger si attiva per ogni riga inserita
BEGIN
    -- Controlla se il valore degli utenti user1 e user2, sono in ordine crescente
    IF NEW.user1 > NEW.user2 THEN
            -- Crea una variabile temporanea per effettuare lo scambio dei valori ed effettua lo scambio
            SET @temp = NEW.user1;
            SET NEW.user1 = NEW.user2;
            SET NEW.user2 = @temp;
END IF;
END $$

-- Ripristina il delimitatore standard
DELIMITER ;

CREATE TABLE message (
    id VARCHAR(255) NOT NULL PRIMARY KEY,
    sender VARCHAR(255) NOT NULL,
    receiver VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    timestamp TIMESTAMP NOT NULL,
    FOREIGN KEY (sender) REFERENCES user(id) ON DELETE CASCADE,
    FOREIGN KEY (receiver) REFERENCES user(id) ON DELETE CASCADE
);

DELIMITER $$

CREATE TRIGGER check_sender_and_receiver_are_in_friendship_table
    BEFORE INSERT ON message
    FOR EACH ROW
BEGIN
    IF NOT EXISTS (SELECT * FROM friendship WHERE (user1 = LEAST(NEW.sender, NEW.receiver) AND user2 = GREATEST(NEW.sender, NEW.receiver))) THEN
            SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Sender and receiver are not friends';
END IF;
END $$

DELIMITER ;
