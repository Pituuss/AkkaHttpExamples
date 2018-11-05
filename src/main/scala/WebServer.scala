import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import akka.util.{ByteString, Timeout}
import akka.{Done, NotUsed}

import spray.json.DefaultJsonProtocol._

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.io.StdIn
import scala.util.Random


object WebServer {
  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  implicit val executionerContext = system.dispatcher


  case class Bid(userId: String, offer: Int)
  case class Bids(bids: List[Bid])
  case class GetBids()

  class Auction extends Actor with ActorLogging {
    var bids = List.empty[Bid]

    override def receive: Receive = {
      case GetBids ⇒
        sender() ! Bids(bids)
      case bid@Bid(userId, offer) ⇒
        bids = bids :+ bid
        log.info(s"Bid complete $userId $offer")
      case _ ⇒ log.info("Invalid message")
    }
  }

  var orders: List[Item] = List[Item]()

  final case class Item(name: String, id: Long)
  final case class Order(items: List[Item])

  implicit val itemFormat = jsonFormat2(Item)
  implicit val orderFormat = jsonFormat1(Order)
  implicit val bidFormat = jsonFormat2(Bid)
  implicit val bidsFormat = jsonFormat1(Bids)

  def fetchItem(itemId: Long): Future[Option[Item]] = Future {
    orders.find(o ⇒ o.id == itemId)
  }

  def saveOrder(order: Order): Future[Done] = {
    orders = order match {
      case Order(items) ⇒ items ::: orders
      case _ ⇒ orders
    }
    Future {
      Done
    }
  }

  val numbers: Source[Int, NotUsed] = Source.fromIterator(() ⇒ Iterator.continually(Random.nextInt()))

  def maino(args: Array[String]): Unit = {

    val auction = system.actorOf(Props[Auction], "auction")

    val route: Route =
      put {
        pathPrefix("auction") {
          parameter("bid".as[Int], "user") { (bid, user) =>
            // place a bid, fire-and-forget
            auction ! Bid(user, bid)
            complete((StatusCodes.Accepted, "bid placed"))
          }
        }
      } ~
        get {
          implicit val timeout: Timeout = 5.seconds

          // query the actor for the current auction state
          val bids: Future[Bids] = (auction ? GetBids).mapTo[Bids]
          complete(bids)
        } ~
        get {
          pathPrefix("item" / LongNumber) { id ⇒
            val maybeItem: Future[Option[Item]] = fetchItem(id)

            onSuccess(maybeItem) {
              case Some(item) ⇒ complete(item)
              case None ⇒ complete(StatusCodes.NotFound)
            }
          }
        } ~
        get {
          pathPrefix("random") {
            complete(HttpEntity(ContentTypes.`text/plain(UTF-8)`, numbers.map(x ⇒ ByteString(s"$x\n"))))
          }
        } ~
        post {
          path("create-order") {
            entity(as[Order]) { order ⇒
              val saved: Future[Done] = saveOrder(order)
              onComplete(saved) { _ ⇒
                complete("order created")
              }
            }
          }
        }

    val bindingFuture = Http().bindAndHandle(route, "localhost", 8080)
    println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
    StdIn.readLine() // let it run until user presses return
    bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ ⇒ system.terminate()) // and shutdown when done

  }
}