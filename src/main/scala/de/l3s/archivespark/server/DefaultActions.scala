package de.l3s.archivespark.server

import de.l3s.archivespark.enrich.EnrichRoot
import de.l3s.archivespark.specific.warc.WarcLikeRecord
import de.l3s.archivespark.implicits._

import scala.reflect._

object DefaultActions {
  import ServerUtil._

  def register[A <: EnrichRoot : ClassTag](actions: TypedActions[A]): Unit = {
    actions.register("enrich", (rdd, funcs, request, queries) => {
      printRdd(enrich(rdd)(funcs).toJsonStrings)
    })

    actions.register("map", (rdd, funcs, request, queries) => singleValueAction(funcs) { (func, field) =>
      printRdd(enrich(rdd)(funcs).mapValues(func, field), tsv)
    })

    actions.register("flatmap", (rdd, funcs, request, queries) => singleValueAction(funcs) { (func, field) =>
      printRdd(enrich(rdd)(funcs).flatMapValues(func, field), tsv)
    })

    if (classOf[WarcLikeRecord].isAssignableFrom(classTag[A].runtimeClass)) {
      registerWarcActions(actions.asInstanceOf[TypedActions[WarcLikeRecord]])
    }
  }

  def registerWarcActions(actions: TypedActions[WarcLikeRecord]): Unit = {
    actions.register("mapCdx", (rdd, funcs, request, queries) => singleValueAction(funcs) { (func, field) =>
      printRdd(enrich(rdd)(funcs).flatMap { r =>
        r.value(func, field).map(str).filter(_.trim.nonEmpty).map(v => r.toCdxString(Seq(v)))
      })
    })

    actions.register("flatmapCdx", (rdd, funcs, request, queries) => singleValueAction(funcs) { (func, field) =>
      printRdd(enrich(rdd)(funcs).flatMap{ r =>
        r.value(func, field).getOrElse(Seq.empty).map(str).filter(_.trim.nonEmpty).map(v => r.toCdxString(Seq(v)))
      })
    })
  }
}
