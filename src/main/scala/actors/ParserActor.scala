package actors

import actors.ParserActor._
import akka.actor.{Actor, ActorLogging, ActorRef}
import org.jsoup.Jsoup

import scala.jdk.CollectionConverters._

class ParserActor(dbActor: ActorRef) extends Actor with ActorLogging {

  private val minimumWordLength = 3
  private val minimumElementTextLength = 10

  override def receive: Receive = { case Body(link, html) =>
    context.parent ! SchedulerActor.NewLinks(link, getLinks(html, link))
    val text = getText(html)
    val words = getWords(text)

    log.debug(s"Link $link contains text $text")

    if (words.nonEmpty) {

      dbActor ! DBActor.Put(
        words = words,
        link = link,
        text = text,
        title = getTitle(html)
      )
    }
  }

  private def getTitle(html: String): String = {

    Jsoup.parse(html).title()
  }

  private def getText(html: String): String = {

    try {

      Jsoup
        .parse(html)
        .getAllElements
        .textNodes()
        .asScala
        .map(_.text)
        .filter(_.length >= minimumElementTextLength)
        .mkString(" ")

    } catch {
      case _: Exception => ""
    }
  }

  private def getWords(text: String): List[(String, Int)] = {

    // Todo: reduce to basic form: shoes -> shoe, ate -> eat
    text
      .split("[[ ]*|[,]*|[;]*|[:]*|[']*|[’]*|[\\\\]*|[\"]*|[.]*|[…]*|[:]*|[/]*|[!]*|[?]*|[+]*]+")
      .toList
      .filter(word => word.length >= minimumWordLength && word.forall(_.isLetter))
      .map(_.toLowerCase)
      .groupBy(identity)
      .view
      .mapValues(_.size)
      .toList
  }

  private def getLinks(html: String, link: String): List[String] = {

    Jsoup
      .parse(html, link)
      .select("a")
      .asScala
      .map(_.absUrl("href"))
      .toList
      .distinct
  }
}

object ParserActor {
  case class Body(link: String, html: String)
}
