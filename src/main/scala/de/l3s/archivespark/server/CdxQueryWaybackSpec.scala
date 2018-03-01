package de.l3s.archivespark.server

import java.net.{URL, URLEncoder}

import de.l3s.archivespark.dataspecs.DataSpec
import de.l3s.archivespark.specific.warc.{CdxRecord, WaybackRecord}
import de.l3s.archivespark.utils.RddUtil
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

import scala.io.Source

class CdxQueryWaybackSpec private(baseUrl: String, queryParams: Map[String, String]) extends DataSpec[String, WaybackRecord] {
  override def load(sc: SparkContext, minPartitions: Int): RDD[String] = {
    val url = Seq(baseUrl, queryParams.map{case (k,v) => s"$k=${URLEncoder.encode(v, "UTF-8")}"}.mkString("&")).mkString("?")
    RddUtil.parallelize(sc, Source.fromURL(url).getLines().toSeq, minPartitions).cache
  }

  override def parse(data: String): Option[WaybackRecord] = {
    CdxRecord.fromString(data).map(cdx => new WaybackRecord(cdx))
  }
}

object CdxQueryWaybackSpec {
  def apply(baseUrl: String, queryParams: Map[String, String] = Map.empty): CdxQueryWaybackSpec = new CdxQueryWaybackSpec(baseUrl, queryParams)
}
