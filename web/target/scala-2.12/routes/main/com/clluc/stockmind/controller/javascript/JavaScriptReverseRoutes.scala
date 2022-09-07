
// @GENERATOR:play-routes-compiler
// @SOURCE:/Users/albertogomeztoribio/git/stockmind/web/conf/routes
// @DATE:Thu Aug 09 13:07:02 CEST 2018

import play.api.routing.JavaScriptReverseRoute



// @LINE:5
package com.clluc.stockmind.controller.javascript {

  // @LINE:16
  class ReverseSendTransactionController(_prefix: => String) {

    def _defaultPrefix: String = {
      if (_prefix.endsWith("/")) "" else "/"
    }

  
    // @LINE:16
    def withdraw: JavaScriptReverseRoute = JavaScriptReverseRoute(
      "com.clluc.stockmind.controller.SendTransactionController.withdraw",
      """
        function() {
          return _wA({method:"POST", url:"""" + _prefix + { _defaultPrefix } + """" + "v1/withdrawals"})
        }
      """
    )
  
    // @LINE:24
    def transfer: JavaScriptReverseRoute = JavaScriptReverseRoute(
      "com.clluc.stockmind.controller.SendTransactionController.transfer",
      """
        function() {
          return _wA({method:"POST", url:"""" + _prefix + { _defaultPrefix } + """" + "v1/transactions"})
        }
      """
    )
  
  }

  // @LINE:5
  class ReverseSocialAuthController(_prefix: => String) {

    def _defaultPrefix: String = {
      if (_prefix.endsWith("/")) "" else "/"
    }

  
    // @LINE:5
    def mobileAuth: JavaScriptReverseRoute = JavaScriptReverseRoute(
      "com.clluc.stockmind.controller.SocialAuthController.mobileAuth",
      """
        function(provider0) {
        
          if (true) {
            return _wA({method:"GET", url:"""" + _prefix + { _defaultPrefix } + """" + "v1/mobileauth/" + encodeURIComponent((""" + implicitly[play.api.mvc.PathBindable[String]].javascriptUnbind + """)("provider", provider0))})
          }
        
        }
      """
    )
  
  }

  // @LINE:14
  class ReverseUsersController(_prefix: => String) {

    def _defaultPrefix: String = {
      if (_prefix.endsWith("/")) "" else "/"
    }

  
    // @LINE:14
    def findUserInfo: JavaScriptReverseRoute = JavaScriptReverseRoute(
      "com.clluc.stockmind.controller.UsersController.findUserInfo",
      """
        function() {
          return _wA({method:"GET", url:"""" + _prefix + { _defaultPrefix } + """" + "v1/users/me"})
        }
      """
    )
  
  }

  // @LINE:36
  class ReverseTwitterController(_prefix: => String) {

    def _defaultPrefix: String = {
      if (_prefix.endsWith("/")) "" else "/"
    }

  
    // @LINE:36
    def queryUser: JavaScriptReverseRoute = JavaScriptReverseRoute(
      "com.clluc.stockmind.controller.TwitterController.queryUser",
      """
        function(query0) {
          return _wA({method:"GET", url:"""" + _prefix + { _defaultPrefix } + """" + "v1/twitter/users/" + encodeURIComponent((""" + implicitly[play.api.mvc.PathBindable[String]].javascriptUnbind + """)("query", query0))})
        }
      """
    )
  
  }

  // @LINE:18
  class ReverseRetrieveTransactionsController(_prefix: => String) {

    def _defaultPrefix: String = {
      if (_prefix.endsWith("/")) "" else "/"
    }

  
    // @LINE:18
    def getTransactionsPage: JavaScriptReverseRoute = JavaScriptReverseRoute(
      "com.clluc.stockmind.controller.RetrieveTransactionsController.getTransactionsPage",
      """
        function() {
          return _wA({method:"GET", url:"""" + _prefix + { _defaultPrefix } + """" + "v1/transactions"})
        }
      """
    )
  
    // @LINE:22
    def getPendingTransaction: JavaScriptReverseRoute = JavaScriptReverseRoute(
      "com.clluc.stockmind.controller.RetrieveTransactionsController.getPendingTransaction",
      """
        function(id0) {
          return _wA({method:"GET", url:"""" + _prefix + { _defaultPrefix } + """" + "v1/transactions/pending/" + encodeURIComponent((""" + implicitly[play.api.mvc.PathBindable[String]].javascriptUnbind + """)("id", id0))})
        }
      """
    )
  
    // @LINE:26
    def cancelPendingTransaction: JavaScriptReverseRoute = JavaScriptReverseRoute(
      "com.clluc.stockmind.controller.RetrieveTransactionsController.cancelPendingTransaction",
      """
        function(id0) {
          return _wA({method:"DELETE", url:"""" + _prefix + { _defaultPrefix } + """" + "v1/transactions/pending/" + encodeURIComponent((""" + implicitly[play.api.mvc.PathBindable[String]].javascriptUnbind + """)("id", id0))})
        }
      """
    )
  
    // @LINE:20
    def getTransaction: JavaScriptReverseRoute = JavaScriptReverseRoute(
      "com.clluc.stockmind.controller.RetrieveTransactionsController.getTransaction",
      """
        function(id0) {
          return _wA({method:"GET", url:"""" + _prefix + { _defaultPrefix } + """" + "v1/transactions/" + encodeURIComponent((""" + implicitly[play.api.mvc.PathBindable[String]].javascriptUnbind + """)("id", id0))})
        }
      """
    )
  
  }

  // @LINE:28
  class ReverseStreamController(_prefix: => String) {

    def _defaultPrefix: String = {
      if (_prefix.endsWith("/")) "" else "/"
    }

  
    // @LINE:29
    def streamEcho: JavaScriptReverseRoute = JavaScriptReverseRoute(
      "com.clluc.stockmind.controller.StreamController.streamEcho",
      """
        function(id0) {
          return _wA({method:"GET", url:"""" + _prefix + { _defaultPrefix } + """" + "v1/stream/echo/" + encodeURIComponent((""" + implicitly[play.api.mvc.PathBindable[String]].javascriptUnbind + """)("id", id0))})
        }
      """
    )
  
    // @LINE:28
    def stream: JavaScriptReverseRoute = JavaScriptReverseRoute(
      "com.clluc.stockmind.controller.StreamController.stream",
      """
        function(id0) {
          return _wA({method:"GET", url:"""" + _prefix + { _defaultPrefix } + """" + "v1/stream/" + encodeURIComponent((""" + implicitly[play.api.mvc.PathBindable[String]].javascriptUnbind + """)("id", id0))})
        }
      """
    )
  
  }

  // @LINE:31
  class ReverseTokensController(_prefix: => String) {

    def _defaultPrefix: String = {
      if (_prefix.endsWith("/")) "" else "/"
    }

  
    // @LINE:31
    def supportedTokens: JavaScriptReverseRoute = JavaScriptReverseRoute(
      "com.clluc.stockmind.controller.TokensController.supportedTokens",
      """
        function() {
          return _wA({method:"GET", url:"""" + _prefix + { _defaultPrefix } + """" + "v1/tokens"})
        }
      """
    )
  
    // @LINE:32
    def createNewToken: JavaScriptReverseRoute = JavaScriptReverseRoute(
      "com.clluc.stockmind.controller.TokensController.createNewToken",
      """
        function() {
          return _wA({method:"POST", url:"""" + _prefix + { _defaultPrefix } + """" + "v1/tokens"})
        }
      """
    )
  
  }


}
