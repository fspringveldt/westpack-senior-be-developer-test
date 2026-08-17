CREATE TABLE account
(
    id                UUID         NOT NULL,
    account_number    VARCHAR(36)  NOT NULL,
    customer_name     VARCHAR(100) NOT NULL,
    account_nick_name VARCHAR(30),
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_account PRIMARY KEY (id),
    CONSTRAINT uk_account_account_number UNIQUE (account_number)
);

CREATE INDEX idx_account_customer_name ON account (LOWER(customer_name));
