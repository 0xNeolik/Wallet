import "./HumanStandardToken.sol";
import "./NFTCollection.sol";

pragma solidity ^0.4.8;

contract TokenFactory {

    mapping(address => address[]) public created;
    mapping(address => bool) public isHumanToken; //verify without having to do a bytecode check.
    //bytes public humanStandardByteCode;

    function TokenFactory() {
      //upon creation of the factory, deploy a HumanStandardToken (parameters are meaningless) and store the bytecode provably.
      //address verifiedToken = createHumanStandardToken(10000, "Verify Token", 3, "VTX");
      //humanStandardByteCode = codeAt(verifiedToken);
    }

    event NewToken(address indexed owner, address token);
    event NewTokenNFT(address indexed owner, address token);
    
    function createHumanStandardToken(uint256 _initialAmount, string _name, uint8 _decimals, string _symbol, address owner) returns (address) {

        HumanStandardToken newToken = (new HumanStandardToken(_initialAmount, _name, _decimals, _symbol));
        created[msg.sender].push(address(newToken));
        isHumanToken[address(newToken)] = true;
        newToken.transfer(owner, _initialAmount); //the factory will own the created tokens. You must transfer them.

        NewToken(owner, address(newToken));

        return address(newToken);
    }
    
    function createNFTCollection(string _name, string _symbol, address owner) public returns (address) {

       NFTCollection newToken = (new NFTCollection(_name, _symbol));
       created[msg.sender].push(address(newToken));
       isHumanToken[address(newToken)] = true;

       NewTokenNFT(owner, address(newToken));

       return address(newToken);
   }
}
