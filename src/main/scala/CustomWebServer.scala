//import akka.actor.Status.Success
import HelloService.HelloMsg
import ServiceGroup.CreateService
import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.util.Timeout

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._
import scala.io.StdIn
import scala.util.{Failure, Success}
//import scala.util.{Failure, Success}

object CustomWebServer {
  implicit val system: ActorSystem = ActorSystem()
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executionerContext: ExecutionContextExecutor = system.dispatcher

  //          implicit val timeout: Timeout = 5.milliseconds
  implicit val timeout: Timeout = 5.seconds

  val serviceGroup: ActorRef = system.actorOf(ServiceGroup.props(), "serviceGroup")

  def helloRoute(): Route = {
    get {
      pathPrefix("start") {
        serviceGroup ! CreateService("dev1")
        complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "<h1>OK</h1>"))
      }
    } ~
      get {
        pathPrefix("hello") {

          val response = (serviceGroup ? HelloService.Hello("dev1")).mapTo[HelloMsg]

          onComplete(response) {
            case Success(HelloMsg(msg, _)) ⇒ complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, msg))
            case Failure(_) ⇒ complete(StatusCodes.NotFound)
          }
        }
      }
  }


  def main(args: Array[String]): Unit = {
    val bindFuture = Http().bindAndHandle(helloRoute(), "localhost", 8080)
    StdIn.readLine()
    bindFuture
      .flatMap(_.unbind())
      .onComplete(_ ⇒ system.terminate())
    system.terminate()
  }
}
