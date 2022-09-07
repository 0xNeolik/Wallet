INSERT INTO users VALUES (
    '08da7a0b-984c-4b05-adbc-d108421cd65a',
    'twitter',
    10821552,
    'Víctor',
    'Villena',
    ''
) ON CONFLICT DO NOTHING;

INSERT INTO users VALUES (
    '3b8e5d0c-3183-4f5b-8a1b-e03a976c830f',
    'twitter',
    292228460,
    'Jose',
    'Arias',
    ''
) ON CONFLICT DO NOTHING;

INSERT INTO users VALUES (
    'd5319295-0dd2-4f79-8497-955219417b60',
    'twitter',
    70995577,
    'Álvaro',
    'Castellanos',
    ''
) ON CONFLICT DO NOTHING;

INSERT INTO twitter_accounts VALUES (
    '08da7a0b-984c-4b05-adbc-d108421cd65a',
    10821552,
    'victorVillenaAb',
    false,
    37,
    'https://pbs.twimg.com/profile_images/588060116110434304/L_keDxiY_normal.jpg'
) ON CONFLICT DO NOTHING;

INSERT INTO twitter_accounts VALUES (
    '3b8e5d0c-3183-4f5b-8a1b-e03a976c830f',
    292228460,
    'JoseAriasF',
    false,
    58,
    'https://pbs.twimg.com/profile_images/1754541047/gaviota-prohibido_normal.jpg'
) ON CONFLICT DO NOTHING;

INSERT INTO twitter_accounts VALUES (
    'd5319295-0dd2-4f79-8497-955219417b60',
    70995577,
    'AlvaroCaste',
    false,
    277,
    'https://pbs.twimg.com/profile_images/787067175580368896/NuLsJQ6__normal.jpg'
) ON CONFLICT DO NOTHING;

INSERT INTO ethereum_accounts VALUES (
    '08da7a0b-984c-4b05-adbc-d108421cd65a',
    '0000000000000000000000000000000000000000',
    'password0'
) ON CONFLICT DO NOTHING;

INSERT INTO ethereum_accounts VALUES (
    '3b8e5d0c-3183-4f5b-8a1b-e03a976c830f',
    '0000000000000000000000000000000000000001',
    'password1'
) ON CONFLICT DO NOTHING;

INSERT INTO ethereum_accounts VALUES (
    'd5319295-0dd2-4f79-8497-955219417b60',
    '0000000000000000000000000000000000000002',
    'password2'
) ON CONFLICT DO NOTHING;

INSERT INTO erc20_tokens VALUES (
    'BLV',
    'Believe',
    0,
    '8b10301e990840cc78eab1ed2d0fcbede8ff219c'
) ON CONFLICT DO NOTHING;

INSERT INTO erc20_transfers 
    (token_symbol, param_from, param_to, param_value, block, tx_hash, tx_index, processed_date)
    VALUES (
        'BLV',
        '0000000000000000000000000000000000000002',
        '0000000000000000000000000000000000000000',
        100,
        0,
        '0000000000000000000000000000000000000000000000000000000000000000',
        0,
        current_timestamp
) ON CONFLICT DO NOTHING;

INSERT INTO erc20_transfers 
    (token_symbol, param_from, param_to, param_value, block, tx_hash, tx_index, processed_date)
    VALUES (
        'BLV',
        '0000000000000000000000000000000000000000',
        '0000000000000000000000000000000000000002',
        100,
        0,
        '0000000000000000000000000000000000000000000000000000000000000001',
        0,
        current_timestamp
) ON CONFLICT DO NOTHING;
