package com.adrianrafo.seed.server.process

import cats.effect.{Sync, Timer}
import cats.syntax.apply._
import cats.syntax.functor._
import com.adrianrafo.seed.server.protocol.people._
import com.adrianrafo.seed.server.protocol.services.PeopleService
import fs2._
import io.chrisdavenport.log4cats.Logger

import scala.concurrent.duration._

class PeopleServiceHandler[F[_]:Timer](implicit F: Sync[F], L: Logger[F]) extends PeopleService[F] {

  val serviceName = "PeopleService"

  def getPerson(request: PeopleRequest): F[PeopleResponse] =
    L.info(s"$serviceName - Request: $request").as(PeopleResponse(Person(request.name, 10)))

  def getPersonStream(request: Stream[F, PeopleRequest]): Stream[F, PeopleResponse] = {

    def responseStream(person: PeopleRequest): Stream[F, PeopleResponse] = {
      val response = PeopleResponse(Person(person.name, 10))
      Stream.awakeEvery[F](2.seconds).evalMap(_ => L
          .info(s"$serviceName - Stream Response: $response")
          .as(response))
    }

    for {
      person   <- request
      _        <- Stream.eval(L.info(s"$serviceName - Stream Request: $person"))
      response <- responseStream(person)
    } yield response
  }

}
