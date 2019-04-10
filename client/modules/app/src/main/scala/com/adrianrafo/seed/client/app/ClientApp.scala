package com.adrianrafo.seed.client.app

import cats.effect._
import com.adrianrafo.seed.client.common.models._
import fs2.Stream
import io.chrisdavenport.log4cats.Logger

import scala.concurrent.ExecutionContext.Implicits.global

class ClientProgram[F[_]: ConcurrentEffect] extends ClientBoot[F] {

  def clientProgram(config: SeedClientConfig)(implicit L: Logger[F]): Stream[F, ExitCode] = {
    for {
      peopleClient <- peopleServiceClient(config.client.host, config.client.port)
      _            <- Stream.eval(peopleClient.getPerson(config.params.request))
      _            <- peopleClient.getRandomPersonStream
    } yield ExitCode.Success
  }
}

object ClientApp extends IOApp {
  def run(args: List[String]): IO[ExitCode] =
    new ClientProgram[IO]
      .runProgram(args)
      .compile
      .toList
      .map(_.headOption.getOrElse(ExitCode.Error))
}
