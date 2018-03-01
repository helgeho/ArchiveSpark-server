import de.l3s.archivespark.ArchiveSpark
import de.l3s.archivespark.enrich.functions._
import de.l3s.archivespark.server.{ArchiveSparkServer, CdxQueryWaybackSpec, ServerUtil}
import de.l3s.archivespark.specific.warc.{CdxRecord, WaybackRecord}
import de.l3s.archivespark.specific.warc.enrichfunctions.WarcPayload
import org.apache.spark.{SparkConf, SparkContext}

ArchiveSparkServer.env.addToken(Html)
ArchiveSparkServer.env.addToken(HtmlAttribute)
ArchiveSparkServer.env.addToken(HtmlText)
ArchiveSparkServer.env.addToken(WarcPayload)
ArchiveSparkServer.env.addToken(Values)
ArchiveSparkServer.env.addToken(SURT)
ArchiveSparkServer.env.addToken(Entities) // requires Stanford's CoreNLP JARs in classpath
ArchiveSparkServer.env.addToken("Title", HtmlText.of(Html.first("title")))
ArchiveSparkServer.env.addToken("Cdx", (field: String) => Root[CdxRecord].map(field) { source: CdxRecord =>
  source.toJson(field)
})

val conf = new SparkConf().setAppName("ArchiveSpark-server").setMaster("local[*]")
val sc = new SparkContext(conf)

val actions = ArchiveSparkServer.init { (action, request, queries) =>
  ArchiveSpark.load(sc, CdxQueryWaybackSpec("http://web.archive.org/cdx/search/cdx", queries))
}

actions.register("links", (rdd, funcs, request, queries) => {
  val Links = Html.all("a")
  val linkProps = Seq(
    Root[CdxRecord].map("surtUrl") { cdx: CdxRecord => cdx.surtUrl },
    Root[CdxRecord].map("timestamp") { cdx: CdxRecord => cdx.timestamp },
    SURT.of(HtmlAttribute("href").ofEach(Links)),
    HtmlText.ofEach(Links)
  )
  val linkValues = Values(linkProps: _*).ofEach(Links)
  ArchiveSparkServer.callAction[WaybackRecord]("flatmap", ServerUtil.enrich(rdd)(linkProps), Seq(linkValues), request, queries)
})

ArchiveSparkServer.start("/archivespark/cdx", 8080)