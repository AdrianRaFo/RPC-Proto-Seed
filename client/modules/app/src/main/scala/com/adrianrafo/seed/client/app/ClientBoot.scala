package com.adrianrafo.seed.client.app

import cats.effect._
import cats.syntax.functor._
import com.adrianrafo.seed.client.common.models.{ClientConfig, SeedClientConfig}
import com.adrianrafo.seed.client.process.runtime.PeopleServiceClient
import com.adrianrafo.seed.config.ConfigService
import fs2.Stream
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import pureconfig.generic.auto._

import scala.concurrent.ExecutionContext

abstract class ClientBoot[F[_]: ConcurrentEffect] {

  def peopleServiceClient(host: String, port: Int)(
      implicit L: Logger[F],
      EC: ExecutionContext): Stream[F, PeopleServiceClient[F]] =
    PeopleServiceClient.createClient(host, port, sslEnabled = false)

  def runProgram(args: List[String]): Stream[F, ExitCode] = {
    def setupConfig: F[SeedClientConfig] =
      ConfigService[F]
        .serviceConfig[ClientConfig]
        .map(client => SeedClientConfig(client, ClientParams.loadParams(client.name, args)))

    def mainStream: Stream[F, ExitCode] =
      for {
        config   <- Stream.eval(setupConfig)
        logger   <- Stream.eval(Slf4jLogger.fromName[F](config.client.name))
        exitCode <- clientProgram(config)(logger)
      } yield exitCode

    mainStream.drain
      .covaryOutput[ExitCode]
      .handleErrorWith(e => Stream.emit(ExitCode.Error)) ++ Stream.emit(ExitCode.Success)
  }

  def clientProgram(config: SeedClientConfig)(implicit L: Logger[F]): Stream[F, ExitCode]

}
