DELIMITER //
    CREATE PROCEDURE criarTabelas()
        BEGIN
CREATE TABLE BankAccount (
            accountNumber VARCHAR(20) NOT NULL PRIMARY KEY,
            accountBalance DECIMAL(15,2) DEFAULT 0,
            clientName VARCHAR(40) NOT NULL,
            NIF INT(9) NOT NULL,
            address VARCHAR(50) NOT NULL,
            zipCode VARCHAR(10) NOT NULL,
            phoneNumber INT(9) NOT NULL,
            email VARCHAR(50),
            birthDate DATE NOT NULL,
            maritalStatus VARCHAR(10) NOT NULL,
            gender VARCHAR(10) NOT NULL
            );

CREATE TABLE Card (
            cardNumber VARCHAR(10) NOT NULL PRIMARY KEY,
            accountNumber VARCHAR(20) UNIQUE NOT NULL,
            cardPIN VARCHAR(4) NOT NULL,

            FOREIGN KEY (accountNumber) REFERENCES BankAccount(accountNumber)
            );

CREATE TABLE Movement (
            movementID INT AUTO_INCREMENT PRIMARY KEY,
            accountNumber VARCHAR(20) NOT NULL,
            movementDate DATETIME NOT NULL,
            movementType VARCHAR(30) NOT NULL,
            movementValue DECIMAL(8,2),
            movementDescription VARCHAR(40) NOT NULL,

            FOREIGN KEY (accountNumber) REFERENCES BankAccount(accountNumber)
            );
        END; //
DELIMITER ;

CALL criarTabelas();