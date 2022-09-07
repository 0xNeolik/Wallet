
// @GENERATOR:play-routes-compiler
// @SOURCE:/Users/albertogomeztoribio/git/stockmind/web/conf/routes
// @DATE:Thu Aug 09 13:07:02 CEST 2018

import play.api.mvc.Call



// @LINE:43
package controllers {

  // @LINE:43
  class ReverseAssets(_prefix: => String) {
    def _defaultPrefix: String = {
      if (_prefix.endsWith("/")) "" else "/"
    }

  
    // @LINE:43
    def at(file:String): Call = {
      
      Call("GET", _prefix + { _defaultPrefix } + "assets/" + implicitly[play.api.mvc.PathBindable[String]].unbind("file", file))
    }
  
  }


}
