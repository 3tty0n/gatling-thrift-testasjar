package io.gatling.thrift.action

import akka.actor.ActorRef
import com.twitter.util.{Future, Return, Throw}
import io.gatling.commons.stats.{KO, OK}
import io.gatling.core.Predef.{Session, Status}
import io.gatling.core.action.{Action, ExitableAction}
import io.gatling.core.action.builder.ActionBuilder
import io.gatling.core.protocol.ProtocolComponentsRegistry
import io.gatling.core.stats.StatsEngine
import io.gatling.core.stats.message.ResponseTimings
import io.gatling.core.structure.ScenarioContext
import io.gatling.core.util.NameGen
import io.gatling.thrift.protocol.ThriftProtocol

class ThriftConnect[A](val statsEngine: StatsEngine, val next: Action, callback: => Future[A])
  extends ExitableAction with NameGen {
  override def name: String = genName("thriftConnect")

  override def execute(session: Session): Unit = {
    val start = System.currentTimeMillis()
    callback.respond {
      case Return(v) =>
        val end = System.currentTimeMillis()
        val timings = ResponseTimings(start, end)
        logger.info(v.toString)
        statsEngine.logResponse(session, "thrift session", timings, OK, None, None)
        next ! session
      case Throw(e) =>
        val end = System.currentTimeMillis()
        val timings = ResponseTimings(start, end)
        logger.info(e.getMessage)
        statsEngine.logResponse(session, "thrift session", timings, KO, None, None)
        next ! session
    }

  }
}

class ThriftActionBuilder[A](callBack: => Future[A]) extends ActionBuilder {

  private def components(protocolComponentsRegistry: ProtocolComponentsRegistry) =
    protocolComponentsRegistry.components(ThriftProtocol.ThriftProtocolKey)

  override def build(ctx: ScenarioContext, next: Action): Action = {
    import ctx._

    val statsEngine = coreComponents.statsEngine
    val thriftComponents = components(protocolComponentsRegistry)
    new ThriftConnect[A](statsEngine, next, callBack)
  }
}

object ThriftActionBuilder {
  def apply[A](callBack: => Future[A]): ThriftActionBuilder[A] =
    new ThriftActionBuilder[A](callBack)

  def call[A](next: ActorRef)(callBack: => Future[A]): ThriftActionBuilder[A] =
    new ThriftActionBuilder[A](callBack)
}
