
// @GENERATOR:play-routes-compiler
// @SOURCE:/Users/albertogomeztoribio/git/stockmind/web/conf/routes
// @DATE:Thu Aug 09 13:07:02 CEST 2018

package router

import play.core.routing._
import play.core.routing.HandlerInvokerFactory._

import play.api.mvc._


class Routes(
  override val errorHandler: play.api.http.HttpErrorHandler, 
  // @LINE:5
  SocialAuthController_6: com.clluc.stockmind.controller.SocialAuthController,
  // @LINE:14
  UsersController_0: com.clluc.stockmind.controller.UsersController,
  // @LINE:16
  SendTransactionController_2: com.clluc.stockmind.controller.SendTransactionController,
  // @LINE:18
  RetrieveTransactionsController_5: com.clluc.stockmind.controller.RetrieveTransactionsController,
  // @LINE:28
  StreamController_4: com.clluc.stockmind.controller.StreamController,
  // @LINE:31
  TokensController_3: com.clluc.stockmind.controller.TokensController,
  // @LINE:36
  TwitterController_1: com.clluc.stockmind.controller.TwitterController,
  // @LINE:43
  Assets_7: controllers.Assets,
  val prefix: String
) extends GeneratedRouter {

   @javax.inject.Inject()
   def this(errorHandler: play.api.http.HttpErrorHandler,
    // @LINE:5
    SocialAuthController_6: com.clluc.stockmind.controller.SocialAuthController,
    // @LINE:14
    UsersController_0: com.clluc.stockmind.controller.UsersController,
    // @LINE:16
    SendTransactionController_2: com.clluc.stockmind.controller.SendTransactionController,
    // @LINE:18
    RetrieveTransactionsController_5: com.clluc.stockmind.controller.RetrieveTransactionsController,
    // @LINE:28
    StreamController_4: com.clluc.stockmind.controller.StreamController,
    // @LINE:31
    TokensController_3: com.clluc.stockmind.controller.TokensController,
    // @LINE:36
    TwitterController_1: com.clluc.stockmind.controller.TwitterController,
    // @LINE:43
    Assets_7: controllers.Assets
  ) = this(errorHandler, SocialAuthController_6, UsersController_0, SendTransactionController_2, RetrieveTransactionsController_5, StreamController_4, TokensController_3, TwitterController_1, Assets_7, "/")

  def withPrefix(prefix: String): Routes = {
    router.RoutesPrefix.setPrefix(prefix)
    new Routes(errorHandler, SocialAuthController_6, UsersController_0, SendTransactionController_2, RetrieveTransactionsController_5, StreamController_4, TokensController_3, TwitterController_1, Assets_7, prefix)
  }

  private[this] val defaultPrefix: String = {
    if (this.prefix.endsWith("/")) "" else "/"
  }

  def documentation = List(
    ("""GET""", this.prefix + (if(this.prefix.endsWith("/")) "" else "/") + """v1/mobileauth/""" + "$" + """provider<[^/]+>""", """com.clluc.stockmind.controller.SocialAuthController.mobileAuth(provider:String)"""),
    ("""GET""", this.prefix + (if(this.prefix.endsWith("/")) "" else "/") + """mobileauth/""" + "$" + """provider<[^/]+>""", """com.clluc.stockmind.controller.SocialAuthController.mobileAuth(provider:String)"""),
    ("""GET""", this.prefix + (if(this.prefix.endsWith("/")) "" else "/") + """v1/users/me""", """com.clluc.stockmind.controller.UsersController.findUserInfo"""),
    ("""POST""", this.prefix + (if(this.prefix.endsWith("/")) "" else "/") + """v1/withdrawals""", """com.clluc.stockmind.controller.SendTransactionController.withdraw"""),
    ("""GET""", this.prefix + (if(this.prefix.endsWith("/")) "" else "/") + """v1/transactions""", """com.clluc.stockmind.controller.RetrieveTransactionsController.getTransactionsPage"""),
    ("""GET""", this.prefix + (if(this.prefix.endsWith("/")) "" else "/") + """v1/transactions/""" + "$" + """id<[^/]+>""", """com.clluc.stockmind.controller.RetrieveTransactionsController.getTransaction(id:String)"""),
    ("""GET""", this.prefix + (if(this.prefix.endsWith("/")) "" else "/") + """v1/transactions/pending/""" + "$" + """id<[^/]+>""", """com.clluc.stockmind.controller.RetrieveTransactionsController.getPendingTransaction(id:String)"""),
    ("""POST""", this.prefix + (if(this.prefix.endsWith("/")) "" else "/") + """v1/transactions""", """com.clluc.stockmind.controller.SendTransactionController.transfer"""),
    ("""DELETE""", this.prefix + (if(this.prefix.endsWith("/")) "" else "/") + """v1/transactions/pending/""" + "$" + """id<[^/]+>""", """com.clluc.stockmind.controller.RetrieveTransactionsController.cancelPendingTransaction(id:String)"""),
    ("""GET""", this.prefix + (if(this.prefix.endsWith("/")) "" else "/") + """v1/stream/""" + "$" + """id<[^/]+>""", """com.clluc.stockmind.controller.StreamController.stream(id:String)"""),
    ("""GET""", this.prefix + (if(this.prefix.endsWith("/")) "" else "/") + """v1/stream/echo/""" + "$" + """id<[^/]+>""", """com.clluc.stockmind.controller.StreamController.streamEcho(id:String)"""),
    ("""GET""", this.prefix + (if(this.prefix.endsWith("/")) "" else "/") + """v1/tokens""", """com.clluc.stockmind.controller.TokensController.supportedTokens"""),
    ("""POST""", this.prefix + (if(this.prefix.endsWith("/")) "" else "/") + """v1/tokens""", """com.clluc.stockmind.controller.TokensController.createNewToken"""),
    ("""GET""", this.prefix + (if(this.prefix.endsWith("/")) "" else "/") + """v1/twitter/users/""" + "$" + """query<[^/]+>""", """com.clluc.stockmind.controller.TwitterController.queryUser(query:String)"""),
    ("""GET""", this.prefix + (if(this.prefix.endsWith("/")) "" else "/") + """assets/""" + "$" + """file<.+>""", """controllers.Assets.at(file:String)"""),
    Nil
  ).foldLeft(List.empty[(String,String,String)]) { (s,e) => e.asInstanceOf[Any] match {
    case r @ (_,_,_) => s :+ r.asInstanceOf[(String,String,String)]
    case l => s ++ l.asInstanceOf[List[(String,String,String)]]
  }}


  // @LINE:5
  private[this] lazy val com_clluc_stockmind_controller_SocialAuthController_mobileAuth0_route = Route("GET",
    PathPattern(List(StaticPart(this.prefix), StaticPart(this.defaultPrefix), StaticPart("v1/mobileauth/"), DynamicPart("provider", """[^/]+""",true)))
  )
  private[this] lazy val com_clluc_stockmind_controller_SocialAuthController_mobileAuth0_invoker = createInvoker(
    SocialAuthController_6.mobileAuth(fakeValue[String]),
    play.api.routing.HandlerDef(this.getClass.getClassLoader,
      "router",
      "com.clluc.stockmind.controller.SocialAuthController",
      "mobileAuth",
      Seq(classOf[String]),
      "GET",
      this.prefix + """v1/mobileauth/""" + "$" + """provider<[^/]+>""",
      """""",
      Seq()
    )
  )

  // @LINE:6
  private[this] lazy val com_clluc_stockmind_controller_SocialAuthController_mobileAuth1_route = Route("GET",
    PathPattern(List(StaticPart(this.prefix), StaticPart(this.defaultPrefix), StaticPart("mobileauth/"), DynamicPart("provider", """[^/]+""",true)))
  )
  private[this] lazy val com_clluc_stockmind_controller_SocialAuthController_mobileAuth1_invoker = createInvoker(
    SocialAuthController_6.mobileAuth(fakeValue[String]),
    play.api.routing.HandlerDef(this.getClass.getClassLoader,
      "router",
      "com.clluc.stockmind.controller.SocialAuthController",
      "mobileAuth",
      Seq(classOf[String]),
      "GET",
      this.prefix + """mobileauth/""" + "$" + """provider<[^/]+>""",
      """""",
      Seq()
    )
  )

  // @LINE:14
  private[this] lazy val com_clluc_stockmind_controller_UsersController_findUserInfo2_route = Route("GET",
    PathPattern(List(StaticPart(this.prefix), StaticPart(this.defaultPrefix), StaticPart("v1/users/me")))
  )
  private[this] lazy val com_clluc_stockmind_controller_UsersController_findUserInfo2_invoker = createInvoker(
    UsersController_0.findUserInfo,
    play.api.routing.HandlerDef(this.getClass.getClassLoader,
      "router",
      "com.clluc.stockmind.controller.UsersController",
      "findUserInfo",
      Nil,
      "GET",
      this.prefix + """v1/users/me""",
      """""",
      Seq()
    )
  )

  // @LINE:16
  private[this] lazy val com_clluc_stockmind_controller_SendTransactionController_withdraw3_route = Route("POST",
    PathPattern(List(StaticPart(this.prefix), StaticPart(this.defaultPrefix), StaticPart("v1/withdrawals")))
  )
  private[this] lazy val com_clluc_stockmind_controller_SendTransactionController_withdraw3_invoker = createInvoker(
    SendTransactionController_2.withdraw,
    play.api.routing.HandlerDef(this.getClass.getClassLoader,
      "router",
      "com.clluc.stockmind.controller.SendTransactionController",
      "withdraw",
      Nil,
      "POST",
      this.prefix + """v1/withdrawals""",
      """""",
      Seq()
    )
  )

  // @LINE:18
  private[this] lazy val com_clluc_stockmind_controller_RetrieveTransactionsController_getTransactionsPage4_route = Route("GET",
    PathPattern(List(StaticPart(this.prefix), StaticPart(this.defaultPrefix), StaticPart("v1/transactions")))
  )
  private[this] lazy val com_clluc_stockmind_controller_RetrieveTransactionsController_getTransactionsPage4_invoker = createInvoker(
    RetrieveTransactionsController_5.getTransactionsPage,
    play.api.routing.HandlerDef(this.getClass.getClassLoader,
      "router",
      "com.clluc.stockmind.controller.RetrieveTransactionsController",
      "getTransactionsPage",
      Nil,
      "GET",
      this.prefix + """v1/transactions""",
      """""",
      Seq()
    )
  )

  // @LINE:20
  private[this] lazy val com_clluc_stockmind_controller_RetrieveTransactionsController_getTransaction5_route = Route("GET",
    PathPattern(List(StaticPart(this.prefix), StaticPart(this.defaultPrefix), StaticPart("v1/transactions/"), DynamicPart("id", """[^/]+""",true)))
  )
  private[this] lazy val com_clluc_stockmind_controller_RetrieveTransactionsController_getTransaction5_invoker = createInvoker(
    RetrieveTransactionsController_5.getTransaction(fakeValue[String]),
    play.api.routing.HandlerDef(this.getClass.getClassLoader,
      "router",
      "com.clluc.stockmind.controller.RetrieveTransactionsController",
      "getTransaction",
      Seq(classOf[String]),
      "GET",
      this.prefix + """v1/transactions/""" + "$" + """id<[^/]+>""",
      """""",
      Seq()
    )
  )

  // @LINE:22
  private[this] lazy val com_clluc_stockmind_controller_RetrieveTransactionsController_getPendingTransaction6_route = Route("GET",
    PathPattern(List(StaticPart(this.prefix), StaticPart(this.defaultPrefix), StaticPart("v1/transactions/pending/"), DynamicPart("id", """[^/]+""",true)))
  )
  private[this] lazy val com_clluc_stockmind_controller_RetrieveTransactionsController_getPendingTransaction6_invoker = createInvoker(
    RetrieveTransactionsController_5.getPendingTransaction(fakeValue[String]),
    play.api.routing.HandlerDef(this.getClass.getClassLoader,
      "router",
      "com.clluc.stockmind.controller.RetrieveTransactionsController",
      "getPendingTransaction",
      Seq(classOf[String]),
      "GET",
      this.prefix + """v1/transactions/pending/""" + "$" + """id<[^/]+>""",
      """""",
      Seq()
    )
  )

  // @LINE:24
  private[this] lazy val com_clluc_stockmind_controller_SendTransactionController_transfer7_route = Route("POST",
    PathPattern(List(StaticPart(this.prefix), StaticPart(this.defaultPrefix), StaticPart("v1/transactions")))
  )
  private[this] lazy val com_clluc_stockmind_controller_SendTransactionController_transfer7_invoker = createInvoker(
    SendTransactionController_2.transfer,
    play.api.routing.HandlerDef(this.getClass.getClassLoader,
      "router",
      "com.clluc.stockmind.controller.SendTransactionController",
      "transfer",
      Nil,
      "POST",
      this.prefix + """v1/transactions""",
      """""",
      Seq()
    )
  )

  // @LINE:26
  private[this] lazy val com_clluc_stockmind_controller_RetrieveTransactionsController_cancelPendingTransaction8_route = Route("DELETE",
    PathPattern(List(StaticPart(this.prefix), StaticPart(this.defaultPrefix), StaticPart("v1/transactions/pending/"), DynamicPart("id", """[^/]+""",true)))
  )
  private[this] lazy val com_clluc_stockmind_controller_RetrieveTransactionsController_cancelPendingTransaction8_invoker = createInvoker(
    RetrieveTransactionsController_5.cancelPendingTransaction(fakeValue[String]),
    play.api.routing.HandlerDef(this.getClass.getClassLoader,
      "router",
      "com.clluc.stockmind.controller.RetrieveTransactionsController",
      "cancelPendingTransaction",
      Seq(classOf[String]),
      "DELETE",
      this.prefix + """v1/transactions/pending/""" + "$" + """id<[^/]+>""",
      """""",
      Seq()
    )
  )

  // @LINE:28
  private[this] lazy val com_clluc_stockmind_controller_StreamController_stream9_route = Route("GET",
    PathPattern(List(StaticPart(this.prefix), StaticPart(this.defaultPrefix), StaticPart("v1/stream/"), DynamicPart("id", """[^/]+""",true)))
  )
  private[this] lazy val com_clluc_stockmind_controller_StreamController_stream9_invoker = createInvoker(
    StreamController_4.stream(fakeValue[String]),
    play.api.routing.HandlerDef(this.getClass.getClassLoader,
      "router",
      "com.clluc.stockmind.controller.StreamController",
      "stream",
      Seq(classOf[String]),
      "GET",
      this.prefix + """v1/stream/""" + "$" + """id<[^/]+>""",
      """""",
      Seq()
    )
  )

  // @LINE:29
  private[this] lazy val com_clluc_stockmind_controller_StreamController_streamEcho10_route = Route("GET",
    PathPattern(List(StaticPart(this.prefix), StaticPart(this.defaultPrefix), StaticPart("v1/stream/echo/"), DynamicPart("id", """[^/]+""",true)))
  )
  private[this] lazy val com_clluc_stockmind_controller_StreamController_streamEcho10_invoker = createInvoker(
    StreamController_4.streamEcho(fakeValue[String]),
    play.api.routing.HandlerDef(this.getClass.getClassLoader,
      "router",
      "com.clluc.stockmind.controller.StreamController",
      "streamEcho",
      Seq(classOf[String]),
      "GET",
      this.prefix + """v1/stream/echo/""" + "$" + """id<[^/]+>""",
      """""",
      Seq()
    )
  )

  // @LINE:31
  private[this] lazy val com_clluc_stockmind_controller_TokensController_supportedTokens11_route = Route("GET",
    PathPattern(List(StaticPart(this.prefix), StaticPart(this.defaultPrefix), StaticPart("v1/tokens")))
  )
  private[this] lazy val com_clluc_stockmind_controller_TokensController_supportedTokens11_invoker = createInvoker(
    TokensController_3.supportedTokens,
    play.api.routing.HandlerDef(this.getClass.getClassLoader,
      "router",
      "com.clluc.stockmind.controller.TokensController",
      "supportedTokens",
      Nil,
      "GET",
      this.prefix + """v1/tokens""",
      """""",
      Seq()
    )
  )

  // @LINE:32
  private[this] lazy val com_clluc_stockmind_controller_TokensController_createNewToken12_route = Route("POST",
    PathPattern(List(StaticPart(this.prefix), StaticPart(this.defaultPrefix), StaticPart("v1/tokens")))
  )
  private[this] lazy val com_clluc_stockmind_controller_TokensController_createNewToken12_invoker = createInvoker(
    TokensController_3.createNewToken,
    play.api.routing.HandlerDef(this.getClass.getClassLoader,
      "router",
      "com.clluc.stockmind.controller.TokensController",
      "createNewToken",
      Nil,
      "POST",
      this.prefix + """v1/tokens""",
      """""",
      Seq()
    )
  )

  // @LINE:36
  private[this] lazy val com_clluc_stockmind_controller_TwitterController_queryUser13_route = Route("GET",
    PathPattern(List(StaticPart(this.prefix), StaticPart(this.defaultPrefix), StaticPart("v1/twitter/users/"), DynamicPart("query", """[^/]+""",true)))
  )
  private[this] lazy val com_clluc_stockmind_controller_TwitterController_queryUser13_invoker = createInvoker(
    TwitterController_1.queryUser(fakeValue[String]),
    play.api.routing.HandlerDef(this.getClass.getClassLoader,
      "router",
      "com.clluc.stockmind.controller.TwitterController",
      "queryUser",
      Seq(classOf[String]),
      "GET",
      this.prefix + """v1/twitter/users/""" + "$" + """query<[^/]+>""",
      """ TODO To be more REST-like, maybe we should rename the "queryuser" part of the URL to "user"
 (the actual REST resource)""",
      Seq()
    )
  )

  // @LINE:43
  private[this] lazy val controllers_Assets_at14_route = Route("GET",
    PathPattern(List(StaticPart(this.prefix), StaticPart(this.defaultPrefix), StaticPart("assets/"), DynamicPart("file", """.+""",false)))
  )
  private[this] lazy val controllers_Assets_at14_invoker = createInvoker(
    Assets_7.at(fakeValue[String]),
    play.api.routing.HandlerDef(this.getClass.getClassLoader,
      "router",
      "controllers.Assets",
      "at",
      Seq(classOf[String]),
      "GET",
      this.prefix + """assets/""" + "$" + """file<.+>""",
      """#####################

 Fake assets to enable certificate installation

#####################""",
      Seq()
    )
  )


  def routes: PartialFunction[RequestHeader, Handler] = {
  
    // @LINE:5
    case com_clluc_stockmind_controller_SocialAuthController_mobileAuth0_route(params@_) =>
      call(params.fromPath[String]("provider", None)) { (provider) =>
        com_clluc_stockmind_controller_SocialAuthController_mobileAuth0_invoker.call(SocialAuthController_6.mobileAuth(provider))
      }
  
    // @LINE:6
    case com_clluc_stockmind_controller_SocialAuthController_mobileAuth1_route(params@_) =>
      call(params.fromPath[String]("provider", None)) { (provider) =>
        com_clluc_stockmind_controller_SocialAuthController_mobileAuth1_invoker.call(SocialAuthController_6.mobileAuth(provider))
      }
  
    // @LINE:14
    case com_clluc_stockmind_controller_UsersController_findUserInfo2_route(params@_) =>
      call { 
        com_clluc_stockmind_controller_UsersController_findUserInfo2_invoker.call(UsersController_0.findUserInfo)
      }
  
    // @LINE:16
    case com_clluc_stockmind_controller_SendTransactionController_withdraw3_route(params@_) =>
      call { 
        com_clluc_stockmind_controller_SendTransactionController_withdraw3_invoker.call(SendTransactionController_2.withdraw)
      }
  
    // @LINE:18
    case com_clluc_stockmind_controller_RetrieveTransactionsController_getTransactionsPage4_route(params@_) =>
      call { 
        com_clluc_stockmind_controller_RetrieveTransactionsController_getTransactionsPage4_invoker.call(RetrieveTransactionsController_5.getTransactionsPage)
      }
  
    // @LINE:20
    case com_clluc_stockmind_controller_RetrieveTransactionsController_getTransaction5_route(params@_) =>
      call(params.fromPath[String]("id", None)) { (id) =>
        com_clluc_stockmind_controller_RetrieveTransactionsController_getTransaction5_invoker.call(RetrieveTransactionsController_5.getTransaction(id))
      }
  
    // @LINE:22
    case com_clluc_stockmind_controller_RetrieveTransactionsController_getPendingTransaction6_route(params@_) =>
      call(params.fromPath[String]("id", None)) { (id) =>
        com_clluc_stockmind_controller_RetrieveTransactionsController_getPendingTransaction6_invoker.call(RetrieveTransactionsController_5.getPendingTransaction(id))
      }
  
    // @LINE:24
    case com_clluc_stockmind_controller_SendTransactionController_transfer7_route(params@_) =>
      call { 
        com_clluc_stockmind_controller_SendTransactionController_transfer7_invoker.call(SendTransactionController_2.transfer)
      }
  
    // @LINE:26
    case com_clluc_stockmind_controller_RetrieveTransactionsController_cancelPendingTransaction8_route(params@_) =>
      call(params.fromPath[String]("id", None)) { (id) =>
        com_clluc_stockmind_controller_RetrieveTransactionsController_cancelPendingTransaction8_invoker.call(RetrieveTransactionsController_5.cancelPendingTransaction(id))
      }
  
    // @LINE:28
    case com_clluc_stockmind_controller_StreamController_stream9_route(params@_) =>
      call(params.fromPath[String]("id", None)) { (id) =>
        com_clluc_stockmind_controller_StreamController_stream9_invoker.call(StreamController_4.stream(id))
      }
  
    // @LINE:29
    case com_clluc_stockmind_controller_StreamController_streamEcho10_route(params@_) =>
      call(params.fromPath[String]("id", None)) { (id) =>
        com_clluc_stockmind_controller_StreamController_streamEcho10_invoker.call(StreamController_4.streamEcho(id))
      }
  
    // @LINE:31
    case com_clluc_stockmind_controller_TokensController_supportedTokens11_route(params@_) =>
      call { 
        com_clluc_stockmind_controller_TokensController_supportedTokens11_invoker.call(TokensController_3.supportedTokens)
      }
  
    // @LINE:32
    case com_clluc_stockmind_controller_TokensController_createNewToken12_route(params@_) =>
      call { 
        com_clluc_stockmind_controller_TokensController_createNewToken12_invoker.call(TokensController_3.createNewToken)
      }
  
    // @LINE:36
    case com_clluc_stockmind_controller_TwitterController_queryUser13_route(params@_) =>
      call(params.fromPath[String]("query", None)) { (query) =>
        com_clluc_stockmind_controller_TwitterController_queryUser13_invoker.call(TwitterController_1.queryUser(query))
      }
  
    // @LINE:43
    case controllers_Assets_at14_route(params@_) =>
      call(params.fromPath[String]("file", None)) { (file) =>
        controllers_Assets_at14_invoker.call(Assets_7.at(file))
      }
  }
}
