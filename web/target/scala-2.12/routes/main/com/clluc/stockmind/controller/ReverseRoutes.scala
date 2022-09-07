
// @GENERATOR:play-routes-compiler
// @SOURCE:/Users/albertogomeztoribio/git/stockmind/web/conf/routes
// @DATE:Thu Aug 09 13:07:02 CEST 2018

import play.api.mvc.Call



// @LINE:5
package com.clluc.stockmind.controller {

  // @LINE:16
  class ReverseSendTransactionController(_prefix: => String) {
    def _defaultPrefix: String = {
      if (_prefix.endsWith("/")) "" else "/"
    }

  
    // @LINE:16
    def withdraw(): Call = {
      
      Call("POST", _prefix + { _defaultPrefix } + "v1/withdrawals")
    }
  
    // @LINE:24
    def transfer(): Call = {
      
      Call("POST", _prefix + { _defaultPrefix } + "v1/transactions")
    }
  
  }

  // @LINE:5
  class ReverseSocialAuthController(_prefix: => String) {
    def _defaultPrefix: String = {
      if (_prefix.endsWith("/")) "" else "/"
    }

  
    // @LINE:5
    def mobileAuth(provider:String): Call = {
    
      (provider: @unchecked) match {
      
        // @LINE:5
        case (provider)  =>
          
          Call("GET", _prefix + { _defaultPrefix } + "v1/mobileauth/" + play.core.routing.dynamicString(implicitly[play.api.mvc.PathBindable[String]].unbind("provider", provider)))
      
      }
    
    }
  
  }

  // @LINE:14
  class ReverseUsersController(_prefix: => String) {
    def _defaultPrefix: String = {
      if (_prefix.endsWith("/")) "" else "/"
    }

  
    // @LINE:14
    def findUserInfo(): Call = {
      
      Call("GET", _prefix + { _defaultPrefix } + "v1/users/me")
    }
  
  }

  // @LINE:36
  class ReverseTwitterController(_prefix: => String) {
    def _defaultPrefix: String = {
      if (_prefix.endsWith("/")) "" else "/"
    }

  
    // @LINE:36
    def queryUser(query:String): Call = {
      
      Call("GET", _prefix + { _defaultPrefix } + "v1/twitter/users/" + play.core.routing.dynamicString(implicitly[play.api.mvc.PathBindable[String]].unbind("query", query)))
    }
  
  }

  // @LINE:18
  class ReverseRetrieveTransactionsController(_prefix: => String) {
    def _defaultPrefix: String = {
      if (_prefix.endsWith("/")) "" else "/"
    }

  
    // @LINE:18
    def getTransactionsPage(): Call = {
      
      Call("GET", _prefix + { _defaultPrefix } + "v1/transactions")
    }
  
    // @LINE:22
    def getPendingTransaction(id:String): Call = {
      
      Call("GET", _prefix + { _defaultPrefix } + "v1/transactions/pending/" + play.core.routing.dynamicString(implicitly[play.api.mvc.PathBindable[String]].unbind("id", id)))
    }
  
    // @LINE:26
    def cancelPendingTransaction(id:String): Call = {
      
      Call("DELETE", _prefix + { _defaultPrefix } + "v1/transactions/pending/" + play.core.routing.dynamicString(implicitly[play.api.mvc.PathBindable[String]].unbind("id", id)))
    }
  
    // @LINE:20
    def getTransaction(id:String): Call = {
      
      Call("GET", _prefix + { _defaultPrefix } + "v1/transactions/" + play.core.routing.dynamicString(implicitly[play.api.mvc.PathBindable[String]].unbind("id", id)))
    }
  
  }

  // @LINE:28
  class ReverseStreamController(_prefix: => String) {
    def _defaultPrefix: String = {
      if (_prefix.endsWith("/")) "" else "/"
    }

  
    // @LINE:29
    def streamEcho(id:String): Call = {
      
      Call("GET", _prefix + { _defaultPrefix } + "v1/stream/echo/" + play.core.routing.dynamicString(implicitly[play.api.mvc.PathBindable[String]].unbind("id", id)))
    }
  
    // @LINE:28
    def stream(id:String): Call = {
      
      Call("GET", _prefix + { _defaultPrefix } + "v1/stream/" + play.core.routing.dynamicString(implicitly[play.api.mvc.PathBindable[String]].unbind("id", id)))
    }
  
  }

  // @LINE:31
  class ReverseTokensController(_prefix: => String) {
    def _defaultPrefix: String = {
      if (_prefix.endsWith("/")) "" else "/"
    }

  
    // @LINE:31
    def supportedTokens(): Call = {
      
      Call("GET", _prefix + { _defaultPrefix } + "v1/tokens")
    }
  
    // @LINE:32
    def createNewToken(): Call = {
      
      Call("POST", _prefix + { _defaultPrefix } + "v1/tokens")
    }
  
  }


}
