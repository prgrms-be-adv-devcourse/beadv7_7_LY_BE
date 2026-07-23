CREATE TABLE hold (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    auction_id      BIGINT          NOT NULL,
    user_id         BIGINT          NOT NULL,
    amount          DECIMAL(19, 0)  NOT NULL,
    held_at         DATETIME(6)     NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT ukHoldAuctionId UNIQUE (auction_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
