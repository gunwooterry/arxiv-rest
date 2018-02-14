package controllers

import javax.inject._

import models._
import org.joda.time._
import org.joda.time.format._
import play.api.libs.json._
import play.api.libs.ws._
import play.api.mvc._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.xml._

@Singleton
class PaperController @Inject()(cc: ControllerComponents, ws: WSClient) extends AbstractController(cc) {
  implicit val dateTimeWrites: Writes[DateTime] = new Writes[DateTime] {
    override def writes(o: DateTime): JsValue = JsString(ISODateTimeFormat.dateTime.print(o))
  }
  implicit val paperWrites: Writes[Paper] = Json.writes[Paper]

  private val baseUrl = "http://export.arxiv.org/api/query"

  def getPaperById(id: String): Action[AnyContent] = Action.async {
    val futureEntries = getFutureEntries("id_list" -> id)
    futureEntries map { entries =>
      val papers = entriesToPapers(entries)
      if (papers.nonEmpty) Ok(Json.toJson(papers.head))
      else NotFound("No paper with this ID.")
    }
  }

  def getPapersBySubject(subject: String, from: Int, max: Int, keyword: Option[String]): Action[AnyContent] = Action.async {
    val subjectFilter = s"cat:$subject"
    val searchQuery =
      if (keyword.isEmpty) subjectFilter
      else Vector(subjectFilter, s"all:${keyword.get}").mkString(" OR ")

    val futureEntries = getFutureEntries(
      "search_query" -> searchQuery,
      "start" -> from.toString,
      "max_results" -> max.toString,
      "sortBy" -> "lastUpdatedDate"
    )
    futureEntries map { entries =>
      val papers = entriesToPapers(entries)
      Ok(Json.toJson(papers))
    }
  }

  def getPapersByKeyword(keyword: String, from: Int, max: Int): Action[AnyContent] = Action.async {
    val futureEntries = getFutureEntries(
      "search_query" -> s"all:$keyword",
      "start" -> from.toString,
      "max_results" -> max.toString,
      "sortBy" -> "lastUpdatedDate"
    )
    futureEntries map { entries =>
      val papers = entriesToPapers(entries)
      Ok(Json.toJson(papers))
    }
  }

  def getPapersByAuthor(author: String, from: Int, max: Int): Action[AnyContent] = Action.async {
    val nameWithoutPunctuation = author.replace(".", "").replace(",", "")
    val splitName = nameWithoutPunctuation.split(" ")
    val standardizedName =
      if (splitName.length == 1) splitName.head
      else splitName.reverse.toList match {
        case Nil => throw new IllegalArgumentException
        case lastName :: others => (lastName :: others.reverse).mkString("_")
      }

    val authorQuery = Vector(standardizedName, splitName.mkString("_")).map("au:" + _).mkString(" OR ")
    val futureEntries = getFutureEntries(
      "search_query" -> authorQuery,
      "start" -> from.toString,
      "max_results" -> max.toString,
      "sortBy" -> "lastUpdatedDate"
    )
    futureEntries map { entries =>
      val papers = entriesToPapers(entries)
      Ok(Json.toJson(papers))
    }
  }

  private def getFutureEntries(parameters: (String, String)*): Future[NodeSeq] = {
    val futureResponse = ws.url(baseUrl).addQueryStringParameters(parameters: _*).get()
    futureResponse.map(_.xml \ "entry")
  }

  private def entriesToPapers(entries: NodeSeq): Seq[Paper] =
    entries flatMap { entry =>
      try {
        Some(entryToPaper(entry))
      }
      catch {
        case _: NoSuchElementException => None
      }
    }

  private def entryToPaper(entry: Node): Paper = {
    val idNodes = entry \ "id"
    if (idNodes.isEmpty) throw new NoSuchElementException

    val id = idNodes.head.text.replace("http://arxiv.org/abs/", "")
    val title = (entry \ "title").head.text
    if (title == "Error") throw new NoSuchElementException

    val authors = for (author <- entry \ "author") yield (author \ "name").head.text
    val subjects = for (subject <- entry \ "category") yield (subject \ "@term").text
    val published = new DateTime((entry \ "published").head.text)
    val updated = new DateTime((entry \ "updated").head.text)
    val summary = (entry \ "summary").head.text.trim.replace('\n', ' ')

    val commentNodes = entry \ "comment"
    val journalRefNodes = entry \ "journal_ref"
    val comment = if (commentNodes.nonEmpty) Some(commentNodes.head.text) else None
    val journalRef = if (journalRefNodes.nonEmpty) Some(journalRefNodes.head.text) else None

    Paper(id, title, authors, subjects, published, updated, summary, comment, journalRef)
  }
}
