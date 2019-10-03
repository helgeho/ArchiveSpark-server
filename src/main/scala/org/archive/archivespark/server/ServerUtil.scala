package org.archive.archivespark.server

import org.apache.spark.rdd.RDD
import org.archive.archivespark.model.{EnrichFunc, EnrichRoot}
import org.json4s.DefaultFormats
import org.json4s.jackson.Serialization.write

import scala.reflect.ClassTag

object ServerUtil {
  implicit val formats: DefaultFormats.type = DefaultFormats

  def str(v: AnyRef): String = write(v).stripPrefix("\"").stripSuffix("\"")
  def tsv(v: AnyRef): String = v match {
    case seq: TraversableOnce[AnyRef] => seq.map(str).map(_.replaceAll("\t", " ")).mkString("\t")
    case seq: Array[AnyRef] => seq.map(str).map(_.replaceAll("\t", " ")).mkString("\t")
    case _ => str(v).replaceAll("\t", " ")
  }

  def singleValueAction[A <: EnrichRoot](funcs: Seq[EnrichFunc[A, _, AnyRef]])(action: EnrichFunc[A, _, AnyRef] => String): String = {
    funcs.reverse.headOption match {
      case Some(func) => action(func)
      case None => ""
    }
  }

  def enrich[A <: EnrichRoot : ClassTag](rdd: RDD[A])(funcs: Seq[EnrichFunc[_ >: A <: EnrichRoot, _, _]]): RDD[A] = {
    var enriched = rdd
    for (func <- funcs) enriched = enriched.enrich(func)
    enriched
  }

  def printRdd[A <: AnyRef](rdd: RDD[A], str: AnyRef => String = _.toString): String = rdd.map(str).filter(_.trim.nonEmpty).collect.mkString("\n")
}
