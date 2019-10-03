package org.archive.archivespark.server

import javax.servlet.http.HttpServletRequest
import org.apache.spark.rdd.RDD
import org.archive.archivespark.model.{EnrichFunc, EnrichRoot}
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.webapp.WebAppContext
import org.scalatra.servlet.ScalatraListener

import scala.reflect.ClassTag

object ArchiveSparkServer {
  type RddLoader[A] = (String, HttpServletRequest, Map[String, String]) => RDD[A]

  private var rddLoader: RddLoader[EnrichRoot] = _
  private var actions: TypedActions[EnrichRoot] = _

  val env = new SecuredEnvironment(this, "env")

  def getEnrichFunctions(definitions: String): Seq[EnrichFunc[EnrichRoot, _, _]] = {
    env.eval(definitions).map(_.asInstanceOf[EnrichFunc[EnrichRoot, _, _]])
  }

  def init[A <: EnrichRoot : ClassTag](rdd: RddLoader[A]): TypedActions[A] = {
    rddLoader = (action, request, queries) => rdd(action, request, queries).asInstanceOf[RDD[EnrichRoot]]
    val typedActions = new TypedActions[A]()
    actions = typedActions.asInstanceOf[TypedActions[EnrichRoot]]
    DefaultActions.register(typedActions)
    typedActions
  }

  def load(action: String, request: HttpServletRequest, queries: Map[String, String]): RDD[EnrichRoot] = rddLoader(action, request, queries)

  def callAction[A <: EnrichRoot : ClassTag](name: String, rdd: => RDD[A], funcs: Seq[EnrichFunc[_ >: A <: EnrichRoot, _, _]], request: HttpServletRequest, queries: Map[String, String]): Any = {
    actions.asInstanceOf[TypedActions[A]].get(name) match {
      case Some(action) => action(rdd, funcs.map(_.asInstanceOf[EnrichFunc[A, _, AnyRef]]), request, queries)
      case None => throw new UnsupportedOperationException("Action " + name + " does not exist.")
    }
  }

  def start(contextPath: String, port: Int): Unit = {
    val server = new Server(port)

    val context = new WebAppContext()
    context.setContextPath(contextPath)
    context.setResourceBase(".")
    context.setInitParameter(ScalatraListener.LifeCycleKey, classOf[ScalatraBootstrap].getCanonicalName)
    context.setEventListeners(Array(new ScalatraListener))

    server.setHandler(context)
    server.start()
    server.join()
  }
}
