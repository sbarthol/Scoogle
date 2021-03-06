package me.sbarthol.actors

import akka.actor.{Actor, ActorRef}
import com.ning.http.client.{AsyncCompletionHandler, AsyncHttpClient, Response}

import java.net.URL
import scala.collection.mutable
import scala.io.Source

class GetterActor(client: AsyncHttpClient, maxConcurrentConnections: Int) extends Actor {

  private val queue = new mutable.Queue[(String, ActorRef)]
  private var counter = 0

  override def receive: Receive = {

    case GetterActor.Links(links) =>
      links.foreach(link => {

        if (counter == maxConcurrentConnections) {
          queue.enqueue((link, sender))
        } else {
          counter = counter + 1
          request(link, sender)
        }
      })

    case GetterActor.Done(link, body, scheduler) =>
      if (queue.nonEmpty) {
        val (front, sender) = queue.dequeue()
        request(front, sender)
      } else {
        counter = counter - 1
      }
      scheduler ! SchedulerActor.Done(link, body)

    case GetterActor.Error(link, error, scheduler) =>
      if (queue.nonEmpty) {
        val (front, sender) = queue.dequeue()
        request(front, sender)
      } else {
        counter = counter - 1
      }
      scheduler ! SchedulerActor.Error(link, error)
  }

  def request(link: String, scheduler: ActorRef): Unit = {

    try {

      val url = new URL(link)
      val protocol = url.getProtocol

      if (protocol == "file") {

        val source = Source.fromFile("./local/" + link.replace("file://", ""), "UTF-8")
        val content = source.mkString
        source.close

        self ! GetterActor.Done(link, content, scheduler)

      } else {

        val request = client.prepareGet(link).build()

        client.executeRequest(
          request,
          new AsyncCompletionHandler[Response]() {
            override def onCompleted(response: Response): Response = {

              if (response.getStatusCode == 200) {
                self ! GetterActor.Done(link, response.getResponseBody(), scheduler)
              } else {
                self ! GetterActor.Error(
                  link,
                  new Exception(s"${response.getStatusCode}: ${response.getStatusText}"),
                  scheduler
                )
              }

              response
            }

            override def onThrowable(t: Throwable): Unit = {
              self ! GetterActor.Error(link, t, scheduler)
            }
          }
        )
      }
    } catch {
      case t: Throwable => self ! GetterActor.Error(link, t, scheduler)
    }
  }
}

object GetterActor {

  case class Done(link: String, body: String, scheduler: ActorRef)
  case class Error(link: String, error: Throwable, scheduler: ActorRef)
  case class Links(links: List[String])
}
