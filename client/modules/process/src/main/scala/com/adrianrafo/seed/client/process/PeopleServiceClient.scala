package com.adrianrafo.seed.client.process

import java.net.InetAddress

import cats.effect._
import cats.syntax.apply._
import cats.syntax.flatMap._
import cats.syntax.functor._
import com.adrianrafo.seed.server.protocol.people._
import com.adrianrafo.seed.server.protocol.services._
import fs2._
import higherkindness.mu.rpc.ChannelForAddress
import higherkindness.mu.rpc.channel.{ManagedChannelInterpreter, UsePlaintext}
import io.chrisdavenport.log4cats.Logger
import io.grpc.{CallOptions, ManagedChannel}

import scala.util.Random

trait PeopleServiceClient[F[_]] {

  def getPerson(name: String): F[Person]

  def getRandomPersonStream: Stream[F, Person]

}
object PeopleServiceClient {

  def apply[F[_]](
      client: PeopleService[F])(implicit F: Effect[F], L: Logger[F]): PeopleServiceClient[F] =
    new PeopleServiceClient[F] {

      val serviceName = "PeopleClient"

      def getPerson(name: String): F[Person] =
        for {
          _      <- L.info(s"")
          result <- client.getPerson(PeopleRequest(name))
          _      <- L.info(s"$serviceName - Request: $name - Result: $result")
        } yield result.person

      def getRandomPersonStream: Stream[F, Person] = {

        def requestStream: Stream[F, PeopleRequest] =
          Stream.iterateEval(PeopleRequest("")) { _ =>
            val req = PeopleRequest(Random.nextPrintableChar().toString)
            F.delay(Thread.sleep(2000)) *> L.info(s"$serviceName Stream Request: $req").as(req)
          }

        for {
          result <- client.getPersonStream(requestStream)
          _      <- Stream.eval(L.info(s"$serviceName Stream Result: $result"))
        } yield result.person
      }

    }

  def createClient[F[_]: ContextShift: Logger](
      hostname: String,
      port: Int,
      sslEnabled: Boolean = true)(
      implicit F: ConcurrentEffect[F]): fs2.Stream[F, PeopleServiceClient[F]] = {

    val channel: F[ManagedChannel] =
      F.delay(InetAddress.getByName(hostname).getHostAddress).flatMap { ip =>
        val channelFor    = ChannelForAddress(ip, port)
        val channelConfig = if (!sslEnabled) List(UsePlaintext()) else Nil
        new ManagedChannelInterpreter[F](channelFor, channelConfig).build
      }

    def clientFromChannel: Resource[F, PeopleService[F]] =
      PeopleService.clientFromChannel(channel, CallOptions.DEFAULT)

    fs2.Stream.resource(clientFromChannel).map(PeopleServiceClient(_))
  }

}
