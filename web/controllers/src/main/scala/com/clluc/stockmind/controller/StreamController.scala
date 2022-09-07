package com.clluc.stockmind.controller

import javax.inject.Inject

import akka.actor.ActorSystem
import akka.stream.scaladsl.Source
import akka.stream.{ActorMaterializer, OverflowStrategy}
import play.api.http.ContentTypes
import play.api.libs.EventSource
import play.api.mvc.{BaseController, ControllerComponents}

import scala.concurrent.Future

class StreamController @Inject()(
    val controllerComponents: ControllerComponents
)(
    implicit
    val system: ActorSystem
) extends BaseController {

  implicit val _: ActorMaterializer = ActorMaterializer()

  def stream(id: String) = Action.async { implicit request =>
    val streamSource = Source
      .actorRef[String](1, OverflowStrategy.dropHead)
      .named(s"stream-$id")
      .via(EventSource.flow)

    Future.successful(Ok.chunked(streamSource).as(ContentTypes.EVENT_STREAM))
  }

  // debug method
  def streamEcho(id: String) = Action.async { implicit request =>
    system.actorSelection(s"user/*/*-stream-$id") ! "echoecho"
    Future.successful(Ok)
  }

}
