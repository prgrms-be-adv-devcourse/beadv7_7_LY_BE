CREATE TABLE deposit (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    user_id         BIGINT          NOT NULL,
    order_id        VARCHAR(255)    NOT NULL,
    payment_key     VARCHAR(255)    NULL,
    amount          DECIMAL(19, 0)  NOT NULL,
    status          VARCHAR(20)     NOT NULL,
    requested_at    DATETIME(6)     NOT NULL,
    approved_at     DATETIME(6)     NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_deposit_orderId UNIQUE (order_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE wallet (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    user_id         BIGINT          NOT NULL,
    amount          DECIMAL(19, 0)  NOT NULL,
    created_at      DATETIME(6)     NOT NULL,
    updated_at      DATETIME(6)     NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_wallet_user_id UNIQUE (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE point_transaction (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    wallet_id       BIGINT          NOT NULL,
    type            VARCHAR(20)     NOT NULL,
    amount          DECIMAL(19, 0)  NOT NULL,
    balance_after   DECIMAL(19, 0)  NOT NULL,
    related_id      BIGINT          NOT NULL,
    occurred_at     DATETIME(6)     NOT NULL,
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
