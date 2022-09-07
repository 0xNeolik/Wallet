package com.clluc.stockmind.core.actor

import akka.actor._
import com.clluc.stockmind.core.actor.AuthTokenCleaner.Clean
import com.clluc.stockmind.port.secondary.AuthTokenPort
import com.typesafe.scalalogging.LazyLogging
import org.joda.time.DateTime

import scala.concurrent.ExecutionContext

/**
  * A job which cleanup invalid auth tokens.
  *
  * @param authTokenPort The auth token service implementation.
  * @param clock The clock implementation.
  */
private[actor] class AuthTokenCleaner(
    authTokenPort: AuthTokenPort,
    clock: () => DateTime
)(
    implicit
    executionContext: ExecutionContext
) extends Actor
    with LazyLogging {

  /**
    * Process the received messages.
    */
  def receive: Receive = {
    case Clean =>
      val start = clock().getMillis
      val msg   = new StringBuffer("\n")
      msg.append("=================================\n")
      msg.append("Start to cleanup auth tokens\n")
      msg.append("=================================\n")
      authTokenPort
        .cleanExpiredAuthTokens()
        .map { deleted =>
          val seconds = (clock().getMillis - start) / 1000
          msg
            .append("Total of %s auth tokens(s) were deleted in %s seconds".format(deleted.length,
                                                                                   seconds))
            .append("\n")
          msg.append("=================================\n")

          msg.append("=================================\n")
          logger.info(msg.toString)
        }
        .recover {
          case e =>
            msg.append("Couldn't cleanup auth tokens because of unexpected error\n")
            msg.append("=================================\n")
            logger.error(msg.toString, e)
        }
  }
}

/**
  * The companion object.
  */
object AuthTokenCleaner {
  case object Clean

  def props(authTokenPort: AuthTokenPort, clock: () => DateTime)(
      implicit executionContext: ExecutionContext): Props =
    Props(
      new AuthTokenCleaner(
        authTokenPort: AuthTokenPort,
        clock
      )
    )
}
