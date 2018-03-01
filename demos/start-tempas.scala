import de.l3s.archivespark.ArchiveSpark
import de.l3s.archivespark.enrich.functions._
import de.l3s.archivespark.server.{ArchiveSparkServer, CdxQueryWaybackSpec, ServerUtil}
import de.l3s.archivespark.specific.warc.{CdxRecord, WaybackRecord}
import de.l3s.archivespark.specific.warc.enrichfunctions.HttpPayload
import de.l3s.archivespark.specific.warc.tempas._
import org.apache.spark.{SparkConf, SparkContext}

ArchiveSparkServer.env.addToken(Html)
ArchiveSparkServer.env.addToken(HtmlAttribute)
ArchiveSparkServer.env.addToken(HtmlText)
ArchiveSparkServer.env.addToken(HttpPayload)
ArchiveSparkServer.env.addToken(Values)
ArchiveSparkServer.env.addToken(SURT)
ArchiveSparkServer.env.addToken(Entities) // requires Stanford's CoreNLP JARs in classpath
ArchiveSparkServer.env.addToken("Title", HtmlText.of(Html.first("title")))
ArchiveSparkServer.env.addToken("Url", Root[TempasYearResult].map("url") { r: TempasYearResult => r.url })
ArchiveSparkServer.env.addToken("Year", Root[TempasYearResult].map("year") { r: TempasYearResult => r.year })

val conf = new SparkConf().setAppName("ArchiveSpark-server").setMaster("local[*]")
val sc = new SparkContext(conf)

val actions = ArchiveSparkServer.init { (action, request, queries) =>
  val query = queries("q")
  val year = queries("year").toInt
  ArchiveSpark.load(sc, TempasWaybackSpec(query, from = year - 1, to = year + 1, pages = 1, resultsPerPage = 10)).filter(_.year == year)
}

actions.register("links", (rdd, funcs, request, queries) => {
  val Links = Html.all("a")
  val linkProps = Seq(
    Root[TempasYearResult].map("url") { r: TempasYearResult => r.url },
    Root[TempasYearResult].map("year") { r: TempasYearResult => r.year },
    HtmlAttribute("href").ofEach(Links),
    HtmlText.ofEach(Links)
  )
  val linkValues = Values(linkProps: _*).ofEach(Links)
  ArchiveSparkServer.callAction[TempasWaybackRecord]("flatmap", ServerUtil.enrich(rdd)(linkProps), Seq(linkValues), request, queries)
})

ArchiveSparkServer.start("/archivespark/tempas", 8081)