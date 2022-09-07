INSERT INTO erc20_tokens VALUES (
    'SMD',
    '$tockmind',
    0,
    'ab9c6b7246cfabe83720a25e5147f3ccb0c0b06c'
) ON CONFLICT DO NOTHING;

INSERT INTO erc20_tokens VALUES (
    'SLD',
    'Solid',
    3,
    '8c3558e24731be9171a51ecede19d6b3abd85e4f'
) ON CONFLICT DO NOTHING;

INSERT INTO erc20_tokens VALUES (
  'ETH',
  'Ether',
  18,
  '0000000000000000000000000000000000000000'
)
