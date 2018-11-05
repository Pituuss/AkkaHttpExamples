import akka.actor.SupervisorStrategy.Stop
import akka.actor.{Actor, ActorLogging, Props}
import spray.json.DefaultJsonProtocol._
import spray.json.RootJsonFormat


object HelloService {

  final case class Hello(deviceId: String)
  final case class HelloMsg(msg: String, deviceId: String)
  final case object Bye

  implicit val responseFormat: RootJsonFormat[HelloMsg] = jsonFormat2(HelloMsg)

  def props(deviceId: String): Props = Props(new HelloService(deviceId))
}

class HelloService(deviceId: String) extends Actor with ActorLogging {

  import HelloService._


  override def receive: Receive = {
    case Hello(_) ⇒
      List.range(1, 1000000).map(x ⇒ x / 3).map(x ⇒ x.toString)
      sender() ! HelloMsg("<h1>Say hello to akka-http</h1>", deviceId)
    case Bye ⇒ Stop
  }

}
