package org.archive.archivespark.server

import org.scalatra.guavaCache.GuavaCache
import org.scalatra.{ActionResult, BadRequest, Ok, ScalatraServlet}

import scala.collection.JavaConverters._

class RequestController extends ScalatraServlet {
  def cache(key: String)(value: => ActionResult): ActionResult = {
    val result = GuavaCache.get(key).getOrElse(value)
    if (result.status.code == 200) GuavaCache.put(key, result, None)
    result
  }

  get("/:action/?") {
    val action = params("action")
    cache(action + "?" + request.queryString) {
      val queries = request.getParameterMap.asScala.flatMap { case (k, v) => v.headOption.map((k, _)) }.toMap

      try {
        lazy val rdd = ArchiveSparkServer.load(action, request, queries)

        Ok(ArchiveSparkServer.callAction(action, rdd, Seq.empty, request, queries))
      } catch {
        case e: Exception => BadRequest("Error: " + e.getMessage.split("\n").headOption.getOrElse(""))
      }
    }
  }

  get("/:action/:funcs") {
    val action = params("action")
    val instructions = params("funcs")
    cache(action + "/" + instructions + "?" + request.queryString) {
      val queries = request.getParameterMap.asScala.flatMap { case (k, v) => v.headOption.map((k, _)) }.toMap

      try {
        val rdd = ArchiveSparkServer.load(action, request, queries)
        val funcs = ArchiveSparkServer.getEnrichFunctions(instructions)

        Ok(ArchiveSparkServer.callAction(action, rdd, funcs, request, queries))
      } catch {
        case e: Exception => BadRequest("Error: " + e.getMessage.split("\n").headOption.getOrElse(""))
      }
    }
  }
}