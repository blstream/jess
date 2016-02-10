package com.blstream.jess.core

import java.util.UUID

import scala.concurrent.{ Promise, Future }

trait StartGameService {

  def startGame: String => Future[JessLink] =
    nick => {
      Promise[String]().success(UUID.randomUUID().toString).future
    }

}
