import akka.http.scaladsl.model.{HttpEntity, ContentTypes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._

import scala.concurrent.ExecutionContextExecutor
import scala.io.Source
import java.time.LocalDateTime
import java.time.Duration

// Define case class to represent Event
case class Event(date: String, ticketLink: String, image: String)

object EventApi {
  def main(args: Array[String]): Unit = {

    // Initialize Akka system
    implicit val system: ActorSystem = ActorSystem("event-api")
    implicit val materializer: ActorMaterializer = ActorMaterializer()
    implicit val executionContext: ExecutionContextExecutor = system.dispatcher

    // Read events from JSON file
    val eventsJson = Source.fromFile("events.json").getLines.mkString
    val events: Map[String, Event] = decode[Map[String, Event]](eventsJson) match {
      case Right(data) => data
      case Left(error) =>
        println(s"Error parsing JSON: $error")
        Map.empty[String, Event]
    }

    // Define the route
    val route: Route =
      path("events") {
        get {
          val startTime = LocalDateTime.now()

          // Simulate some processing (e.g., returning event data)
          val eventsData = events.asJson.noSpaces

          // Log information
          val endTime = LocalDateTime.now()
          val duration = Duration.between(startTime, endTime)
          val logInfo =
            s"""
               |HTTP Status Code: 200
               |Start Time: $startTime
               |End Time: $endTime
               |Duration: ${duration.toMillis} ms
             """.stripMargin

          // Respond with both event data and log info
          complete(
            HttpEntity(ContentTypes.`application/json`, eventsData)
          )
        }
      }

    // Start the server
    val bindingFuture = Http().newServerAt("localhost", 8080).bind(route)

    bindingFuture.onComplete {
      case scala.util.Success(binding) =>
        println(s"Server started at http://${binding.localAddress.getHostString}:${binding.localAddress.getPort}")
      case scala.util.Failure(exception) =>
        println(s"Failed to bind HTTP server: $exception")
        system.terminate()
    }
  }
}
