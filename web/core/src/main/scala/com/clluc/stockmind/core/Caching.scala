package com.clluc.stockmind.core

import java.util.concurrent.TimeUnit

import com.google.common.cache.{Cache, CacheBuilder, RemovalListener, RemovalNotification}
import com.typesafe.scalalogging.LazyLogging

private[core] object Caching extends LazyLogging {

  def memoize[I, O](fx: I => O)(cache: Cache[I, O]): I => O = { input =>
    cache.get(input, () => fx(input))
  }

  // Context bounds required for Java to Scala compatibility
  def stdCacheInstance[I <: Object, O <: Object](ttlInSeconds: Long = 30L): Cache[I, O] =
    CacheBuilder
      .newBuilder()
      .maximumSize(10000L)
      .expireAfterAccess(ttlInSeconds, TimeUnit.SECONDS)
      .expireAfterWrite(ttlInSeconds, TimeUnit.SECONDS)
      .removalListener(new RemovalListener[I, O] {
        override def onRemoval(notification: RemovalNotification[I, O]) = {
          val wasEvicted = notification.wasEvicted()
          logger.debug("Removed elements from cache")
          logger.debug(s"Evicted: $wasEvicted")
        }
      })
      .recordStats()
      .build[I, O]()
}
