pragma solidity ^0.4.15;

import './ERC721.sol';
import './ERC721Metadata.sol';

contract NFTCollection is ERC721, ERC721Metadata{

    
    function NFTCollection(string name, string symbol) ERC721Metadata(name, symbol)  {
    }
    
    function createNFT(address to, string metadata) public {
        //Generate pseudo-random value
         uint256 tokenId = (uint256(keccak256(now, msg.sender, block.timestamp)));
    
        _mint(to, tokenId);
        _setTokenURI(tokenId, metadata);
    }
    
    function deleteNFT(address from, uint256 tokenId) public {
        _burn(from, tokenId);
    }
}