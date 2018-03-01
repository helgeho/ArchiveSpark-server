package de.l3s.archivespark.server

import de.l3s.archivespark.enrich.{DefaultFieldAccess, EnrichFunc, EnrichRoot}
import org.apache.spark.rdd.RDD
import org.json4s.DefaultFormats
import org.json4s.jackson.Serialization.write
import de.l3s.archivespark.implicits._

import scala.reflect.ClassTag

object ServerUtil {
  implicit val formats: DefaultFormats.type = DefaultFormats

  def str(v: AnyRef): String = write(v).stripPrefix("\"").stripSuffix("\"")
  def tsv(v: AnyRef): String = v match {
    case seq: TraversableOnce[AnyRef] => seq.map(str).map(_.replaceAll("\t", " ")).mkString("\t")
    case seq: Array[AnyRef] => seq.map(str).map(_.replaceAll("\t", " ")).mkString("\t")
    case _ => str(v).replaceAll("\t", " ")
  }

  def defaultField(func: EnrichFunc[_, _]): String = {
    func match {
      case f: DefaultFieldAccess[_, _] => f.defaultField
      case _ => func.fields.head
    }
  }

  def singleValueAction[A <: EnrichRoot](funcs: Seq[EnrichFunc[A, _]])(action: (EnrichFunc[A, _], String) => String): String = {
    funcs.reverse.headOption match {
      case Some(func) => action(func, defaultField(func))
      case None => ""
    }
  }

  def enrich[A <: EnrichRoot : ClassTag](rdd: RDD[A])(funcs: Seq[EnrichFunc[_ >: A <: EnrichRoot, _]]): RDD[A] = {
    var enriched = rdd
    for (func <- funcs) enriched = enriched.enrich(func)
    enriched
  }

  def printRdd[A <: AnyRef](rdd: RDD[A], str: AnyRef => String = _.toString): String = rdd.map(str).filter(_.trim.nonEmpty).collect.mkString("\n")
}
