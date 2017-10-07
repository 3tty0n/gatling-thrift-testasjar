package simulation

import java.util.concurrent.atomic.AtomicBoolean

import com.twitter.finagle.Thrift
import com.twitter.util.Future
import io.gatling.core.Predef._
import io.gatling.core.structure.ScenarioBuilder
import io.gatling.core.session.Session
import io.gatling.thrift.Predef._
import io.gatling.thrift.protocol.ThriftProtocol
import org.micchon.ping.thriftscala.PingService

import scala.util.Random
import scala.concurrent.duration._

class ThriftSimulationExample extends ThriftSimulation {
  val client: PingService.FutureIface =
    Thrift.client.newIface[PingService.FutureIface]("localhost:9911")

  implicit val thriftProtocol: ThriftProtocol =
    thrift.port(9911).host("localhost").requestName("example request")

  private val random = new Random()

  val feeder = Array(
    Map("foo" -> "foo1", "bar" -> "bar1"),
    Map("foo" -> "foo2", "bar" -> "bar2"),
    Map("foo" -> "foo3", "bar" -> "bar3")
  ).random

  object CallBacks {
    private val isDone = new AtomicBoolean(false)

    // https://github.com/3tty0n/gatling-thrift/issues/9
    // This function arises OK once and makes rest of them KO
    def callbackIssue9: Session => Future[String] = { _ =>
      if (isDone.compareAndSet(false, true)) {
        Future.value("first")
      } else {
        Future.exception(new RuntimeException("already done"))
      }
    }

    def callbackSimple: Session => Future[String] = { session =>
      client.echo(session("randNum").as[Int].toString)
    }

    def callbackIssue10: Session => Future[String] = { session =>
      client.echo(session("i").as[String])
    }

    def callBackFeeder: Session => Future[String] = { session =>
      client.echo(session("foo").as[String])
    }
  }

  val scn: ScenarioBuilder = scenario("Thrift Scenario")
    .exec { session =>
      session.setAll(
        ("randNum", random.nextInt()),
        ("i", "session support")
      )
    }
    .feed(feeder)
    .exec { CallBacks.callBackFeeder.action }
    .exec { CallBacks.callbackSimple.action }
    .exec { CallBacks.callbackIssue10.action }
  //.exec { CallBacks.callbackIssue9.action }

  setUp(scn.inject(nothingFor(4 seconds), atOnceUsers(100)))
}
