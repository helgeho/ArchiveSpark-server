package org.archive.archivespark.server

import javax.servlet.ServletContext

import org.scalatra.LifeCycle

class ScalatraBootstrap extends LifeCycle {
  override def init(context: ServletContext): Unit = context.mount(new RequestController, "/*")
}
