package com.blstream.jess
package core.score

import java.nio.ByteBuffer

import akka.actor.{ ActorRef, ActorSystem, Props }
import akka.http.scaladsl.model.ws.{ BinaryMessage, Message }
import akka.stream.actor.ActorPublisher
import akka.stream.scaladsl._
import akka.util.ByteString
import com.typesafe.scalalogging.LazyLogging

import concurrent.duration._

trait ScoreService extends LazyLogging {

  def system: ActorSystem

  def scoreRouter: ActorRef

  def scoreFlow: Flow[Message, Message, _] = {
    val actor = system.actorOf(Props(classOf[ScorePublisher], scoreRouter))
    val ap = ActorPublisher[ScoreRouter.IncommingMessage](actor)

    val toApi: Flow[ScoreRouter.IncommingMessage, protocol.WsApi, _] = Flow[ScoreRouter.IncommingMessage].map {
      case ScoreRouter.Join(nick) => protocol.PlayerJoinsGame(nick)
      case ScoreRouter.Score(nick, score) => protocol.PlayerScoresPoint(nick, score)
    }

    val serialize: Flow[protocol.WsApi, ByteString, _] = Flow[protocol.WsApi].map { msg =>
      {
        val encode: ByteBuffer = protocol.encode(msg)
        ByteString(encode)
      }
    }
    val src = Source
      .fromPublisher(ap)
      .via(toApi)
      .keepAlive(10.seconds, () => protocol.Ping(System.currentTimeMillis()))
      .via(serialize)

    Flow.fromSinkAndSource(Sink.ignore, src.map {
      api => BinaryMessage.Strict(api)
    })

  }

}
