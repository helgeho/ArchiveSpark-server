import java.net.URLEncoder

import org.apache.spark.{SparkConf, SparkContext}
import org.archive.archivespark._
import org.archive.archivespark.functions._
import org.archive.archivespark.model.pointers.FieldPointer
import org.archive.archivespark.server.{ArchiveSparkServer, ServerUtil}
import org.archive.archivespark.sparkling.cdx.CdxRecord
import org.archive.archivespark.specific.warc._
import org.archive.archivespark.specific.warc.functions.WarcPayload

ArchiveSparkServer.env.addToken(Html)
ArchiveSparkServer.env.addToken(HtmlAttribute)
ArchiveSparkServer.env.addToken(HtmlText)
ArchiveSparkServer.env.addToken(WarcPayload)
ArchiveSparkServer.env.addToken(Values)
ArchiveSparkServer.env.addToken(SURT)
ArchiveSparkServer.env.addToken(Entities) // requires Stanford's CoreNLP JARs in classpath
ArchiveSparkServer.env.addToken("Title", HtmlText.of(Html.first("title")))
ArchiveSparkServer.env.addToken("Cdx", (field: String) => FieldPointer.root[WarcLikeRecord, CdxRecord].mapEnrichable(field) { root =>
  val cdx = root.get
  Map[String, String](
    "surtUrl" -> cdx.surtUrl,
    "timestamp" -> cdx.timestamp,
    "originalUrl" -> cdx.originalUrl,
    "mime" -> cdx.mime,
    "status" -> cdx.status.toString,
    "digest" -> cdx.digest,
    "redirectUrl" -> cdx.redirectUrl,
    "meta" -> cdx.meta,
    "compressedSize" -> cdx.compressedSize.toString
  )(field)
})

val conf = new SparkConf().setAppName("ArchiveSpark-server").setMaster("local[*]")
val sc = new SparkContext(conf)

val actions = ArchiveSparkServer.init { (action, request, queries) =>
  val url = "http://web.archive.org/cdx/search/cdx?" + queries.map{case (k,v) => s"$k=${URLEncoder.encode(v, "UTF-8")}"}.mkString("&")
  ArchiveSpark.load(WarcSpec.fromWaybackByCdxQuery(url))
}

actions.register("links", (rdd, funcs, request, queries) => {
  val Links = Html.all("a")
  val linkProps = Seq(
    FieldPointer.root[WarcLikeRecord, CdxRecord].map("surtUrl")(_.surtUrl),
    FieldPointer.root[WarcLikeRecord, CdxRecord].map("timestamp") { cdx: CdxRecord => cdx.timestamp },
    SURT.of(HtmlAttribute("href").ofEach(Links)),
    HtmlText.ofEach(Links)
  )
  val linkValues = Values(linkProps: _*).ofEach(Links)
  ArchiveSparkServer.callAction[WaybackRecord]("flatmap", ServerUtil.enrich(rdd)(linkProps), Seq(linkValues), request, queries)
})

ArchiveSparkServer.start("/archivespark/cdx", 8080)