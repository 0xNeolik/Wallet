package com.clluc.stockmind.adapter.ethereum

import play.api.libs.ws.WSResponse

private[ethereum] object FakeWSResponse {

  def createFakeResponse(bodyContent: String): WSResponse = new WSResponse {
    override def statusText           = ???
    override def underlying[T]        = ???
    override def xml                  = ???
    override def body                 = bodyContent
    override def header(key: String)  = ???
    override def cookie(name: String) = ???
    override def bodyAsBytes          = ???
    override def cookies              = ???
    override def status               = ???
    override def json                 = ???
    override def allHeaders           = ???
    override def headers              = ???
    override def bodyAsSource         = ???
  }
}
