-- Chủ: N04. Ba bảng dùng chung của nhóm.
CREATE DATABASE IF NOT EXISTS chess CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE chess;

CREATE TABLE IF NOT EXISTS tblPlayer (
    id        INT AUTO_INCREMENT PRIMARY KEY,
    username  VARCHAR(50)  NOT NULL UNIQUE,
    password  VARCHAR(64)  NOT NULL,          -- SHA-256 hex
    name      VARCHAR(100) NOT NULL,
    email     VARCHAR(100),
    tel       VARCHAR(20)
);

CREATE TABLE IF NOT EXISTS tblMatch (
    id        INT AUTO_INCREMENT PRIMARY KEY,
    startTime DATETIME NOT NULL,
    endTime   DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS tblMatchResult (
    id        INT AUTO_INCREMENT PRIMARY KEY,
    matchId   INT NOT NULL,
    playerId  INT NOT NULL,
    result    VARCHAR(10) NOT NULL,            -- WIN | DRAW | LOSE
    color     VARCHAR(10) NOT NULL,            -- WHITE | BLACK
    UNIQUE KEY uq_match_player (matchId, playerId),
    FOREIGN KEY (matchId)  REFERENCES tblMatch(id) ON DELETE CASCADE,
    FOREIGN KEY (playerId) REFERENCES tblPlayer(id)
);
