package org.archive.archivespark.server

import scala.reflect.runtime.universe._

class TypedFactory private (factory: () => Any, val tpe: Type) {
  def get: Any = factory()
}

object TypedFactory {
  def apply[A : TypeTag](factory: => A): TypedFactory = new TypedFactory(() => factory, typeTag[A].tpe)
}