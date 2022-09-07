## How to support external tokens to our platform:

This page describes the process to support external tokens showing how to add and receive an example token.

**Create MetaMask account**

First of all, we need an external account that will hold the tokens during the test.

In order to create this account, we need to add the MetaMask Google Chrome extension.

[[images/InstallMetamask.png]]

Click `add extension`.

[[images/InstallMetamask2.png]]

Click the MetaMask icon.

[[images/SelectMetamask.png]]

Read and agree to the MetaMask terms and Conditions.

[[images/MetamaskTerms.png]]

Set the password.

[[images/MetamaskCreateAccount.png]]

Metamask will now show the seed. It is very important to copy and store those 12 worlds, without them the wallet cannot be restored.

[[images/MetamaskSafeCode.png]]

And the account is now created.

[[images/MetamaskCreated.png]]

The last step to get the MetaMask account ready is to select the correct Network. Ropsten in our case.

[[images/SelectRopsten.png]]

**Get tokens**

The second step is getting tokens to do this test. The token selected in this case is the BOKKY token.

To get BOKKYs we need to send ETHER to the Smart Contract that controls this token. (0x583cbBb8a8443B38aBcC0c956beCe47340ea1367)

[[images/GetTokenFromContract.png]]

In a few minutes, we will receive the tokens, but first the tokens have to be added in MetaMask.

[[images/AddTokkenMetaMask.png]]

And we can see the new tokens.

[[images/TokenBalance.png]]

We are now ready to send the tokens to our platform, but this contract has not been added yet.

**Add tokens to the platform**

The platform is continuously watching the blockchain listening for events related to the tokens added.

First of all we need to add the token to the database. The contract address has to be in lowercase.

 ```
  INSERT INTO erc20_tokens VALUES (
    'BOKKY',
    '$BokkyPooBah Test',
    18,
    '583cbbb8a8443b38abcc0c956bece47340ea1367'
) ON CONFLICT DO NOTHING;
```

And restart the application to start watching the token.

```
sudo service stockmind-api restart
```

**Receive tokens**

After the restart, the application can see Transfer events throwed by the BOKKY Smart Contract.

To test this, we will send BOKKY tokens to one of our Stockmind accounts. MyEtherWallet can be used to do it:  [here](https://www.myetherwallet.com/#send-transaction).

[[images/LoginMyEtherWallet.png]]

Before sending the token, we need to add it.

[[images/AddTokenMyEther.png]]
 Add token

And send it to our personal address in Stockmind.

[[images/WalletAddress.png]]

[[images/SendTokenToStockmind.png]]

We can see that new BOKKY tokens have arrived and we can send them back.

[[images/ReceivedFromExternal.png]]

[[images/SendTokenToExternalWallet.png]]

Receiving again the tokens previously sent.

[[images/FinalBalance.png]]
