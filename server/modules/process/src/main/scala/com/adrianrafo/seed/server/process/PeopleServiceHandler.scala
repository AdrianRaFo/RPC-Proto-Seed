package com.adrianrafo.seed.server.process

import cats.effect.Sync
import cats.syntax.applicative._
import cats.syntax.apply._
import cats.syntax.functor._
import com.adrianrafo.seed.protocol.people._
import fs2._
import io.chrisdavenport.log4cats.Logger

class PeopleServiceHandler[F[_]: Sync](implicit L: Logger[F]) extends PeopleService[F] {

  val serviceName = "PeopleService"

  def getPerson(request: PeopleRequest): F[PeopleResponse] =
    L.info(s"$serviceName - Request: $request").as(PeopleResponse(Person(request.name, 10)))

  def getPersonStream(request: Stream[F, PeopleRequest]): Stream[F, PeopleResponse] = {

    def responseF(person: PeopleRequest): F[PeopleResponse] = {
      val response = PeopleResponse(Person(person.name, 10))
      Thread.sleep(2000).pure[F] *> L.info(s"$serviceName - Response: $response").as(response)
    }

    for {
      person   <- request
      _        <- Stream.eval(L.info(s"$serviceName - Request: $person"))
      response <- Stream.eval(responseF(person))
    } yield response
  }

}
