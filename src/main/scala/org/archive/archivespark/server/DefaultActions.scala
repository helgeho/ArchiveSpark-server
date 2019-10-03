package org.archive.archivespark.server

import org.archive.archivespark.specific.warc.WarcLikeRecord
import org.archive.archivespark.model.EnrichRoot

import scala.reflect._

object DefaultActions {
  import ServerUtil._

  def register[A <: EnrichRoot : ClassTag](actions: TypedActions[A]): Unit = {
    actions.register("enrich", (rdd, funcs, request, queries) => {
      printRdd(enrich(rdd)(funcs).toJsonStrings)
    })

    actions.register("map", (rdd, funcs, request, queries) => singleValueAction(funcs) { func =>
      printRdd(enrich(rdd)(funcs).mapValues(func), tsv)
    })

    actions.register("flatmap", (rdd, funcs, request, queries) => singleValueAction(funcs) { func =>
      printRdd(enrich(rdd)(funcs).flatMapValues(func.multi), tsv)
    })

    if (classOf[WarcLikeRecord].isAssignableFrom(classTag[A].runtimeClass)) {
      registerWarcActions(actions.asInstanceOf[TypedActions[WarcLikeRecord]])
    }
  }

  def registerWarcActions(actions: TypedActions[WarcLikeRecord]): Unit = {
    actions.register("mapCdx", (rdd, funcs, request, queries) => singleValueAction(funcs) { func =>
      printRdd(enrich(rdd)(funcs).flatMap { r =>
        r.value(func).map(str).filter(_.trim.nonEmpty).map(v => r.toCdxString(Seq(v)))
      })
    })

    actions.register("flatmapCdx", (rdd, funcs, request, queries) => singleValueAction(funcs) { func =>
      printRdd(enrich(rdd)(funcs).flatMap{ r =>
        r.value(func.multi).getOrElse(Seq.empty).map(str).filter(_.trim.nonEmpty).map(v => r.toCdxString(Seq(v)))
      })
    })
  }
}
