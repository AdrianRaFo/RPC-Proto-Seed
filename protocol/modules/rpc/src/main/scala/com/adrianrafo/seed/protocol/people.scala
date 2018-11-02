package com.adrianrafo.seed.protocol

import fs2._
import mu.rpc.protocol._

object people {

  @message
  case class Person(name: String, age: Int)

  @message
  case class PeopleRequest(name: String)

  @message
  case class PeopleResponse(person: Person)

  @service(Protobuf)
  trait PeopleService[F[_]] {

    def getPerson(request: PeopleRequest): F[PeopleResponse]
    def getPersonStream(request: Stream[F, PeopleRequest]): Stream[F, PeopleResponse]

  }

}
