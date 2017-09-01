package io.gatling.thrift.protocol

import akka.actor.ActorSystem
import io.gatling.core.CoreComponents
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.protocol.{ProtocolComponents, ProtocolKey}
import io.gatling.core.session.Session

class ThriftProtocol(address: String, port: Int) {

  val ThriftProtocolKey = new ProtocolKey {
    override type Protocol = ThriftProtocol
    override type Components = ThriftComponents

    override def protocolClass =
      classOf[ThriftProtocol]
        .asInstanceOf[Class[io.gatling.core.protocol.Protocol]]

    override def defaultProtocolValue(
      configuration: GatlingConfiguration
    ): Protocol =
      new ThriftProtocol(address, port)

    override def newComponents(
      system: ActorSystem,
      coreComponents: CoreComponents
    ): ThriftProtocol => ThriftComponents = { thriftProtocol =>
      ThriftComponents(thriftProtocol)
    }
  }
}

object ThriftProtocol {

  def apply(address: String, port: Int): ThriftProtocol =
    new ThriftProtocol(address, port)
}

case class ThriftComponents(thriftProtocol: ThriftProtocol)
    extends ProtocolComponents {
  override def onStart: Option[(Session) => Session] = None

  override def onExit: Option[(Session) => Unit] = None
}
