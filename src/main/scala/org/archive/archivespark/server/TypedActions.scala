package org.archive.archivespark.server

import javax.servlet.http.HttpServletRequest
import org.apache.spark.rdd.RDD
import org.archive.archivespark.model.{EnrichFunc, EnrichRoot}

import scala.reflect.ClassTag

class TypedActions[A <: EnrichRoot : ClassTag] {
  type Action = (=> RDD[A], Seq[EnrichFunc[A, _, AnyRef]], HttpServletRequest, Map[String, String]) => Any

  private var actions: Map[String, Action] = Map.empty

  def disable(name: String): TypedActions[A] = {
    actions -= name
    this
  }

  def register(name: String, action: Action): TypedActions[A] = {
    actions = actions.updated(name, action)
    this
  }

  def get(name: String): Option[Action] = actions.get(name)
}