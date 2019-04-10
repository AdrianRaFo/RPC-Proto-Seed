package com.adrianrafo.seed.server.app

import cats.effect._
import cats.syntax.flatMap._
import cats.syntax.functor._
import com.adrianrafo.seed.protocol.people._
import com.adrianrafo.seed.server.common.models._
import com.adrianrafo.seed.server.process.PeopleServiceHandler
import higherkindness.mu.rpc.server.{AddService, GrpcServer}
import io.chrisdavenport.log4cats.Logger

import scala.concurrent.ExecutionContext.Implicits.global

class ServerProgram[F[_]: ConcurrentEffect] extends ServerBoot[F] {

  def serverProgram(config: ServerConfig)(implicit L: Logger[F]): F[ExitCode] = {

    val serverName = s"${config.name}"

    implicit val PS: PeopleService[F] = new PeopleServiceHandler[F]

    for {
      peopleService <- PeopleService.bindService[F]
      server        <- GrpcServer.default[F](config.port, List(AddService(peopleService)))
      _             <- L.info(s"$serverName - Starting server at ${config.host}:${config.port}")
      exitCode      <- GrpcServer.server(server).as(ExitCode.Success)
    } yield exitCode

  }
}

object ServerApp extends IOApp {
  def run(args: List[String]): IO[ExitCode] = new ServerProgram[IO].runProgram(args)
}
