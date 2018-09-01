package com.adrianrafo.seed.client.process.runtime

import cats.effect._
import cats.syntax.applicative._
import cats.syntax.apply._
import cats.syntax.flatMap._
import cats.syntax.functor._
import com.adrianrafo.seed.client.process.ClientRPC
import com.adrianrafo.seed.protocol.people._
import fs2._
import io.chrisdavenport.log4cats.Logger
import io.grpc.{CallOptions, ManagedChannel}
import monix.execution.Scheduler

import scala.concurrent.duration.FiniteDuration
import scala.util.Random

trait PeopleServiceClient[F[_]] {

  def getPerson(name: String): F[Person]

  def getRandomPersonStream: Stream[F, Person]

}
object PeopleServiceClient {

  def apply[F[_]: Effect](clientF: F[PeopleService.Client[F]])(
      implicit L: Logger[F]): PeopleServiceClient[F] =
    new PeopleServiceClient[F] {

      val serviceName = "PeopleClient"

      def getPerson(name: String): F[Person] =
        for {
          client <- clientF
          _      <- L.info(s"Request: $name")
          result <- client.getPerson(PeopleRequest(name))
          _      <- L.info(s"Result: $result")
        } yield result.person

      def getRandomPersonStream: Stream[F, Person] = {

        def requestStream: Stream[F, PeopleRequest] =
          Stream.iterateEval(PeopleRequest("")) { _ =>
            val req = PeopleRequest(Random.nextPrintableChar().toString)
            Thread.sleep(2000).pure[F] *> L.info(s"$serviceName Request: $req").as(req)
          }

        for {
          client <- Stream.eval(clientF)
          result <- client.getPersonStream(requestStream)
          _      <- Stream.eval(L.info(s"$serviceName Result: $result"))
        } yield result.person
      }

    }

  def createClient[F[_]](
      hostname: String,
      port: Int,
      sslEnabled: Boolean = true,
      tryToRemoveUnusedEvery: FiniteDuration,
      removeUnusedAfter: FiniteDuration)(
      implicit F: Effect[F],
      L: Logger[F],
      TM: Timer[F],
      S: Scheduler): fs2.Stream[F, PeopleServiceClient[F]] = {

    def fromChannel(channel: ManagedChannel): PeopleService.Client[F] =
      PeopleService.clientFromChannel(channel, CallOptions.DEFAULT)

    ClientRPC
      .clientCache(
        (hostname, port).pure[F],
        sslEnabled,
        tryToRemoveUnusedEvery,
        removeUnusedAfter,
        fromChannel)
      .map(cache => PeopleServiceClient(cache.getClient))
  }

}
