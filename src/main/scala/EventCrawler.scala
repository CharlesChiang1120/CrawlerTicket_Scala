import org.jsoup.Jsoup
import scala.jdk.CollectionConverters._
import java.io.{File, PrintWriter}
import java.time.LocalDateTime
import java.time.Duration
import scala.util.{Failure, Success, Try}

object EventCrawler {
  def main(args: Array[String]): Unit = {
    val userAgents = List(
      "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/117.0.0.0 Safari/537.36",
      "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:90.0) Gecko/20100101 Firefox/90.0",
      "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/113.0.0.0 Safari/537.36"
    )

    val url = "https://kktix.com/"
    val startTime = LocalDateTime.now()

    val userAgent = userAgents(scala.util.Random.nextInt(userAgents.length))

    // Perform the HTTP request
    val responseTry = Try {
      Jsoup.connect(url)
        .userAgent(userAgent)
        .timeout(10000)
        .get()
    }

    // End time and duration calculation
    val endTime = LocalDateTime.now()
    val duration = Duration.between(startTime, endTime)

    responseTry match {
      case Success(response) =>
        val httpStatusCode = response.connection().response().statusCode()
        val eventElements = response.select("ul li a")

        val eventsDict = eventElements.iterator().asScala.flatMap { eventElement =>
          Try {
            val activityInfo = eventElement.select("figure figcaption div div div div h2").text()
            val time = eventElement.select("span.date").text()
            val ticketLink = eventElement.attr("href")
            val imageUrl = eventElement.select("figure img").attr("src")
            if (activityInfo.nonEmpty && time.nonEmpty) {
              Some(activityInfo -> Map("date" -> time, "ticketLink" -> ticketLink, "image" -> imageUrl))
            } else None
          }.toOption
        }.flatten.toMap

        // Save the events to a JSON file
        val jsonFile = new File("events.json")
        val writer = new PrintWriter(jsonFile)
        writer.write(ujson.write(eventsDict, indent = 4))
        writer.close()

        // Logging to file
        val logFile = new File("crawler.log")
        val logWriter = new PrintWriter(logFile)
        logWriter.write(s"HTTP Status Code: $httpStatusCode\n")
        logWriter.write(s"Start Time: $startTime\n")
        logWriter.write(s"End Time: $endTime\n")
        logWriter.write(s"Duration: ${duration.getSeconds} seconds\n")
        logWriter.close()

      case Failure(exception) =>
        println(s"Error fetching the page: ${exception.getMessage}")
    }
  }
}
