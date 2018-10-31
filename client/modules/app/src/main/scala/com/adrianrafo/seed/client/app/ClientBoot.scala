package com.adrianrafo.seed.client.app

import cats.effect._
import com.adrianrafo.seed.common.SeedConfig
import com.adrianrafo.seed.config.ConfigService
import fs2.{Stream, StreamApp}
import fs2.StreamApp.ExitCode
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import monix.execution.Scheduler

abstract class ClientBoot[F[_]: Effect] extends StreamApp[F] {

  implicit val S: Scheduler = monix.execution.Scheduler.Implicits.global

  implicit val TM: Timer[F] = Timer.derive[F](Effect[F], IO.timer(S))

  override def stream(args: List[String], requestShutdown: F[Unit]): Stream[F, ExitCode] = {

    def mainStream: Stream[F, ExitCode] =
      for {
        config   <- ConfigService[F].serviceConfig[SeedConfig]
        logger   <- Stream.eval(Slf4jLogger.fromName[F](config.name))
        exitCode <- serverStream(config)(logger)
      } yield exitCode

    mainStream.drain.handleErrorWith(e => Stream.emit(ExitCode.Error)) ++ Stream.emit(
      ExitCode.Success)
  }

  def serverStream(config: SeedConfig)(implicit L: Logger[F]): Stream[F, ExitCode]

}
