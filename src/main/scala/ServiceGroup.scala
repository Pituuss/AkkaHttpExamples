import HelloService.{Hello, HelloMsg}
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.pattern.{ask, pipe}
import akka.util.Timeout

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._

object ServiceGroup {
  def props(): Props = Props(new ServiceGroup())
  final case class CreateService(deviceId: String)
}

class ServiceGroup extends Actor with ActorLogging {

  import ServiceGroup._

  private val idToService = Map.empty[String, ActorRef]

  override def receive: Receive = onMessage(idToService)

  implicit val timeout: Timeout = 5.seconds
  implicit val executionerContext: ExecutionContextExecutor = context.dispatcher

  private def onMessage(idToService: Map[String, ActorRef]): Receive = {
    case CreateService(deviceId) ⇒
      idToService.get(deviceId) match {
        case None ⇒
          val actorRef = context.actorOf(HelloService.props(deviceId), deviceId)
          context.become(onMessage(idToService + (deviceId → actorRef)))
        case Some(_) ⇒
          log.info("service already online")
      }

    case Hello(deviceId) ⇒
      idToService.get(deviceId) match {
        case Some(actorRef) ⇒
          (actorRef ? Hello(deviceId)) pipeTo sender()
        case None ⇒
          sender() ! HelloMsg("<h1>Service is offline</h1>", deviceId)
      }
  }
}
