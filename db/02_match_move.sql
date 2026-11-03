-- Chủ: N11. Biên bản nước đi của ván trực tuyến. Chạy sau 01_core.sql.
USE chess;

CREATE TABLE IF NOT EXISTS tblMatchMove (
    id         INT AUTO_INCREMENT PRIMARY KEY,
    matchId    INT NOT NULL,
    moveIndex  INT NOT NULL,                   -- bắt đầu từ 1; lẻ = Trắng, chẵn = Đen
    fromSquare TINYINT NOT NULL,               -- 0 = a8 ... 63 = h1
    toSquare   TINYINT NOT NULL,
    notation   VARCHAR(10) NOT NULL,
    promotion  CHAR(1) NULL,                   -- Q | R | B | N
    UNIQUE KEY uq_match_move (matchId, moveIndex),
    FOREIGN KEY (matchId) REFERENCES tblMatch(id) ON DELETE CASCADE
);
