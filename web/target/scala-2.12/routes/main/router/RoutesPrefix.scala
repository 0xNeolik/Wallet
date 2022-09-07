
// @GENERATOR:play-routes-compiler
// @SOURCE:/Users/albertogomeztoribio/git/stockmind/web/conf/routes
// @DATE:Thu Aug 09 13:07:02 CEST 2018


package router {
  object RoutesPrefix {
    private var _prefix: String = "/"
    def setPrefix(p: String): Unit = {
      _prefix = p
    }
    def prefix: String = _prefix
    val byNamePrefix: Function0[String] = { () => prefix }
  }
}
