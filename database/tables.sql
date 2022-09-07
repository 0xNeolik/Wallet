
CREATE TABLE IF NOT EXISTS users (
    id             UUID    PRIMARY KEY,
    login_provider VARCHAR NOT NULL,
    login_key      VARCHAR NOT NULL,
    data           JSONB   NOT NULL,
    identifier     VARCHAR,
    CONSTRAINT no_multiaccount UNIQUE (login_provider, login_key)
);

-- TODO Remove create table for twitter_accounts
-- We don't do it now to avoid the code to be unusable while we make progress on the refactor

CREATE TABLE IF NOT EXISTS twitter_accounts (
    user_id        UUID    PRIMARY KEY REFERENCES users(id),
    account_id     BIGINT  NOT NULL UNIQUE,
    screenname     VARCHAR NOT NULL UNIQUE,
    verified       BOOLEAN NOT NULL,
    followers      INTEGER NOT NULL,
    avatar_url     VARCHAR
);

CREATE TABLE IF NOT EXISTS local_directory (
    user_id UUID NOT NULL UNIQUE,
    directory_provider VARCHAR,
    directory_key VARCHAR,
    data JSONB NOT NULL,
    PRIMARY KEY (directory_provider, directory_key)
);

CREATE TABLE IF NOT EXISTS ethereum_accounts (
    user_id        UUID       PRIMARY KEY REFERENCES users (id),
    address        VARCHAR    NOT NULL UNIQUE,
    password       VARCHAR    NOT NULL
);

CREATE TABLE IF NOT EXISTS api_keys (
    user_id        UUID    REFERENCES users (id),
    api_key        UUID    NOT NULL UNIQUE,
    PRIMARY KEY(user_id,api_key)
);

CREATE TABLE IF NOT EXISTS auth_tokens (
    token_id       UUID       PRIMARY KEY,
    user_id        UUID       NOT NULL REFERENCES users (id),
    expiry         TIMESTAMP  NOT NULL
);

CREATE TABLE IF NOT EXISTS oauth1_info (
    token_id       VARCHAR NOT NULL,
    token_secret   VARCHAR NOT NULL,
    token          VARCHAR NOT NULL,
    secret         VARCHAR NOT NULL,
    PRIMARY KEY (token_id, token_secret)
);

CREATE TABLE IF NOT EXISTS cas_info (
    login_provider VARCHAR NOT NULL,
    login_key      VARCHAR NOT NULL,
    ticket         VARCHAR NOT NULL,
    PRIMARY KEY (login_provider, login_key)
);

CREATE TABLE IF NOT EXISTS erc_tokens (
    erc_type         VARCHAR NOT NULL,
    symbol           VARCHAR NOT NULL,
    token_name       VARCHAR NOT NULL,
    contract_address VARCHAR NOT NULL UNIQUE,
    owner            VARCHAR, -- applies to tokens created from the app
    birth_block      INT, -- applies to tokens created from the app
    PRIMARY KEY (erc_type, symbol),
    UNIQUE(erc_type, symbol)
);

CREATE TABLE IF NOT EXISTS erc20_tokens (
    erc_type       VARCHAR NOT NULL,
    token_symbol   VARCHAR NOT NULL PRIMARY KEY,
    decimals       INTEGER NOT NULL CHECK (decimals >= 0 or decimals <= 18),
    CONSTRAINT fk_type_symbol
    FOREIGN KEY (erc_type, token_symbol)
    REFERENCES erc_tokens(erc_type, symbol)
);

CREATE TABLE IF NOT EXISTS erc721_tokens (
    erc_type       VARCHAR NOT NULL,
    token_symbol   VARCHAR NOT NULL,
    meta           VARCHAR,
    id             NUMERIC(80, 0) NOT NULL,
    CONSTRAINT fk_type_symbol
    FOREIGN KEY (erc_type, token_symbol)
    REFERENCES erc_tokens(erc_type, symbol),
    PRIMARY KEY(erc_type,token_symbol,id),
    UNIQUE(erc_type, token_symbol, id)
);


-- Stores Transfer events. The full signature of the event is:
-- event Transfer(address indexed _from, address indexed _to, uint256 _value)
CREATE TABLE IF NOT EXISTS erc_transfers (
    id             BIGSERIAL      PRIMARY KEY,
    token_symbol   VARCHAR        NOT NULL ,
    erc_type       VARCHAR        NOT NULL ,
    param_from     VARCHAR        NOT NULL,
    param_to       VARCHAR        NOT NULL,
    param_value    NUMERIC(80, 0) NOT NULL,
    block          INTEGER        NOT NULL CHECK (block >= 0),
    tx_hash        VARCHAR        NOT NULL UNIQUE,
    tx_index       INTEGER        NOT NULL CHECK (tx_index >= 0),
    processed_date TIMESTAMP,
    token_id            NUMERIC(80, 0),
    CONSTRAINT fk_type_symbol
    FOREIGN KEY (erc_type, token_symbol)
    REFERENCES erc_tokens(erc_type, symbol)
);

CREATE TABLE IF NOT EXISTS inbound_transfers (
    first_step  BIGINT  NOT NULL UNIQUE REFERENCES erc_transfers (id),
    second_step VARCHAR NOT NULL UNIQUE
);

-- Common sequence for offchain and outbound transfers
CREATE SEQUENCE IF NOT EXISTS transfers_common_id;

-- Offchain transfers are done between users of our platform. It also tracks movements
-- between users' addresses and the master address.
CREATE TABLE IF NOT EXISTS offchain_transfers (
id                  BIGINT         DEFAULT nextval('transfers_common_id') PRIMARY KEY,
token_symbol        VARCHAR        NOT NULL ,
erc_type            VARCHAR        NOT NULL ,
from_addr           VARCHAR        NOT NULL,
to_addr             VARCHAR        NOT NULL,
amount              NUMERIC(80, 0) NOT NULL,
created_date        TIMESTAMP      NOT NULL,
onchain_transfer_id BIGINT         REFERENCES erc_transfers (id),
token_id            NUMERIC(80, 0),
    CONSTRAINT fk_type_symbol
    FOREIGN KEY (erc_type, token_symbol)
    REFERENCES erc_tokens(erc_type, symbol)
);

CREATE TABLE IF NOT EXISTS outbound_transfers (
tx_hash              VARCHAR PRIMARY KEY,
offchain_transfer_id BIGINT  REFERENCES offchain_transfers (id)
);


-- Pending transfers are stored. They are settled when the receiving user logs in
-- for the first time.
CREATE TABLE IF NOT EXISTS pending_transfers (
    id                BIGINT         DEFAULT nextval('transfers_common_id') PRIMARY KEY,
    from_id           UUID           NOT NULL REFERENCES users (id),
    to_login_provider VARCHAR        NOT NULL,
    to_login_key      VARCHAR        NOT NULL,
    token_symbol      VARCHAR        NOT NULL,
    erc_type          VARCHAR        NOT NULL,
    amount            NUMERIC(80, 0) NOT NULL,
    created           TIMESTAMP      NOT NULL,
    processed         TIMESTAMP,
    token_id          NUMERIC(80, 0),
    CONSTRAINT fk_type_symbol
    FOREIGN KEY (erc_type, token_symbol)
    REFERENCES erc_tokens(erc_type, symbol)
);

CREATE TABLE IF NOT EXISTS appconfig (
    key            VARCHAR    PRIMARY KEY,
    value          VARCHAR    NOT NULL
);


CREATE OR REPLACE VIEW offchain_balances AS
SELECT total_transfers.eth_address,
    total_transfers.erc_type,
    total_transfers.token,
    sum(total_transfers.total_sent) AS total_sent,
    sum(total_transfers.total_received) AS total_received,
    sum(total_transfers.total_withheld) AS total_withheld,
    sum(total_transfers.total_received) - sum(total_transfers.total_sent) AS real_balance,
    sum(total_transfers.total_received) - sum(total_transfers.total_sent) - sum(total_transfers.total_withheld) AS effective_balance
   FROM ( SELECT COALESCE(received.t, sent.f, pending.f) AS eth_address,
            COALESCE(received.erc_type, sent.erc_type, pending.erc_type) AS erc_type,
            COALESCE(received.token, sent.token, pending.token) AS token,
            COALESCE(sent.sent, 0::numeric) AS total_sent,
            COALESCE(received.received, 0::numeric) AS total_received,
            COALESCE(pending.withheld, 0::numeric) AS total_withheld
           FROM ( SELECT offchain_transfers.from_addr AS f,
                    offchain_transfers.erc_type,
                    offchain_transfers.token_symbol AS token,
                    sum(offchain_transfers.amount) AS sent
                   FROM offchain_transfers
                   WHERE offchain_transfers.erc_type='ERC-20'
                  GROUP BY offchain_transfers.from_addr, offchain_transfers.erc_type, offchain_transfers.token_symbol) sent
             FULL JOIN ( SELECT offchain_transfers.to_addr AS t,
                    offchain_transfers.erc_type,
                    offchain_transfers.token_symbol AS token,
                    sum(offchain_transfers.amount) AS received
                   FROM offchain_transfers
                   WHERE offchain_transfers.erc_type='ERC-20'
                  GROUP BY offchain_transfers.to_addr, offchain_transfers.erc_type, offchain_transfers.token_symbol) received ON sent.f::text = received.t::text AND sent.token::text = received.token::text AND sent.erc_type::text = received.erc_type::text
             FULL JOIN ( SELECT pending_with_eth_account.f,
                    pending_with_eth_account.erc_type,
                    pending_with_eth_account.token,
                    sum(pending_with_eth_account.amount) AS withheld
                   FROM ( SELECT ethereum_accounts.address AS f,
                            pending_transfers.erc_type,
                            pending_transfers.token_symbol AS token,
                            pending_transfers.amount,
                            pending_transfers.processed
                           FROM pending_transfers,
                            ethereum_accounts
                          WHERE pending_transfers.erc_type='ERC-20' AND pending_transfers.from_id = ethereum_accounts.user_id) pending_with_eth_account
                  WHERE pending_with_eth_account.processed IS NULL
                  GROUP BY pending_with_eth_account.f, pending_with_eth_account.erc_type, pending_with_eth_account.token) pending ON sent.f::text = pending.f::text AND sent.token::text = pending.token::text AND sent.erc_type::text = pending.erc_type::text) total_transfers
  GROUP BY total_transfers.eth_address, total_transfers.erc_type, total_transfers.token;


-- View: public.offchain_721balances
CREATE OR REPLACE VIEW public.offchain_721balances AS
 SELECT total_transfers.eth_address,
    total_transfers.erc_type,
    total_transfers.token,
    sum(total_transfers.total_sent) AS total_sent,
    sum(total_transfers.total_received) AS total_received,
    sum(total_transfers.total_withheld) AS total_withheld,
    sum(total_transfers.total_received) - sum(total_transfers.total_sent) AS real_balance,
    sum(total_transfers.total_received) - sum(total_transfers.total_sent) - sum(total_transfers.total_withheld) AS effective_balance,
    total_transfers.tokenid
   FROM ( SELECT COALESCE(received.t, sent.f, pending.f) AS eth_address,
            COALESCE(received.erc_type, sent.erc_type, pending.erc_type) AS erc_type,
            COALESCE(received.token, sent.token, pending.token) AS token,
            COALESCE(sent.sent::numeric, 0::numeric) AS total_sent,
            COALESCE(received.received::numeric, 0::numeric) AS total_received,
            COALESCE(pending.withheld, 0::numeric) AS total_withheld,
            COALESCE(received.tokenid, sent.tokenid, pending.tokenid) AS tokenid
           FROM ( SELECT offchain_transfers.from_addr AS f,
                    offchain_transfers.erc_type,
                    offchain_transfers.token_symbol AS token,
                    1 AS sent,
                    erc721_tokens.id AS tokenid
                   FROM offchain_transfers
                     LEFT JOIN erc721_tokens ON offchain_transfers.token_id::text = erc721_tokens.id::text AND offchain_transfers.erc_type::text = erc721_tokens.erc_type::text AND offchain_transfers.token_symbol::text = erc721_tokens.token_symbol::text
                  GROUP BY offchain_transfers.from_addr, offchain_transfers.erc_type, offchain_transfers.token_symbol, erc721_tokens.id) sent
             FULL JOIN ( SELECT offchain_transfers.to_addr AS t,
                    offchain_transfers.erc_type,
                    offchain_transfers.token_symbol AS token,
                    1 AS received,
                    erc721_tokens.id AS tokenid
                   FROM offchain_transfers
                     LEFT JOIN erc721_tokens ON offchain_transfers.token_id::text = erc721_tokens.id::text AND offchain_transfers.erc_type::text = erc721_tokens.erc_type::text AND offchain_transfers.token_symbol::text = erc721_tokens.token_symbol::text
                  GROUP BY offchain_transfers.to_addr, offchain_transfers.erc_type, offchain_transfers.token_symbol, erc721_tokens.id) received ON sent.f::text = received.t::text AND sent.token::text = received.token::text AND sent.erc_type::text = received.erc_type::text AND sent.tokenid::text = received.tokenid::text
             FULL JOIN ( SELECT pending_with_eth_account.f,
                    pending_with_eth_account.erc_type,
                    pending_with_eth_account.token,
                    pending_with_eth_account.tokenid,
                    sum(pending_with_eth_account.amount) AS withheld
                   FROM ( SELECT ethereum_accounts.address AS f,
                            pending_transfers.erc_type,
                            pending_transfers.token_symbol AS token,
                            pending_transfers.amount,
                            pending_transfers.processed,
                            erc721_tokens.id AS tokenid
                           FROM pending_transfers
                             LEFT JOIN erc721_tokens ON pending_transfers.token_id::text = erc721_tokens.id::text AND pending_transfers.erc_type::text = erc721_tokens.erc_type::text AND pending_transfers.token_symbol::text = erc721_tokens.token_symbol::text
                             LEFT JOIN ethereum_accounts ON pending_transfers.from_id = ethereum_accounts.user_id) pending_with_eth_account
                  WHERE pending_with_eth_account.processed IS NULL
                  GROUP BY pending_with_eth_account.f, pending_with_eth_account.erc_type, pending_with_eth_account.token, pending_with_eth_account.tokenid) pending ON sent.f::text = pending.f::text AND sent.token::text = pending.token::text AND sent.tokenid::text = pending.tokenid::text AND sent.erc_type::text = pending.erc_type::text) total_transfers
  GROUP BY total_transfers.eth_address, total_transfers.erc_type, total_transfers.token, total_transfers.tokenid;

ALTER TABLE public.offchain_721balances
  OWNER TO postgres;

-- TODO Update this view when the twitter_accounts table is deleted
CREATE OR REPLACE VIEW full_user_info AS
SELECT
u.id, u.login_provider, u.login_key, u.data,
eth.address AS eth_address, eth.password AS eth_password
FROM users AS u
LEFT JOIN ethereum_accounts AS eth ON u.id = eth.user_id;

CREATE TABLE IF NOT EXISTS transfers_metadata (
  tx_id BIGINT  REFERENCES offchain_transfers (id),
  key   VARCHAR NOT NULL,
  value VARCHAR NOT NULL,
  PRIMARY KEY (tx_id, key)
);

CREATE OR REPLACE FUNCTION insert_twitter_screename()
  RETURNS trigger AS
$BODY$
BEGIN
UPDATE users
SET identifier = new.screenname
WHERE id=new.user_id;
 RETURN NEW;
END;
$BODY$
language plpgsql;


CREATE TRIGGER update_identifier
AFTER INSERT ON twitter_accounts
FOR EACH ROW
EXECUTE PROCEDURE insert_twitter_screename();

-- Create ETH token (hardcoded, will represent ether)
INSERT INTO erc_tokens
  (erc_type, symbol, token_name,                           contract_address)
VALUES
  ('ERC-20', 'ETH', 'Ether', '0000000000000000000000000000000000000000')
ON CONFLICT DO NOTHING;

INSERT INTO erc20_tokens
  (erc_type, token_symbol, decimals)
VALUES
  ('ERC-20', 'ETH', 18)
ON CONFLICT DO NOTHING;

