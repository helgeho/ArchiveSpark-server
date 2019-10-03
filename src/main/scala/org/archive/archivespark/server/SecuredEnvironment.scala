package org.archive.archivespark.server

import scala.reflect._
import scala.reflect.runtime.universe._
import scala.tools.reflect.ToolBox

class SecuredEnvironment(obj: Any, field: String) {
  private val accessor = obj.getClass.getCanonicalName.stripSuffix("$") + "." + field

  private val tokenStart = """[^0-9\"\-\.\(\;]"""
  private val tokenEnd = """[\.\(\{\)\}\,\;]"""

  private val reflection = scala.reflect.runtime.currentMirror.mkToolBox()

  private var dictionary = Map.empty[String, TypedFactory]

  def addToken[A : TypeTag](name: String, factory: => A): Unit = dictionary = dictionary.updated(name, TypedFactory(factory))
  def addToken[A : TypeTag : ClassTag](factory: => A): Unit = {
    val name = classTag[A].runtimeClass.getSimpleName.stripSuffix("$")
    addToken(name, factory)
  }

  def token(name: String): TypedFactory = dictionary(name)

  private def packToken(token: String): String = {
    val trim = token.trim
    if (trim.isEmpty) ""
    else {
      dictionary.get(trim) match {
        case Some(t) =>
          val typeName = t.tpe.toString
          "(" + accessor + s""".token("$token").get.asInstanceOf[$typeName])"""
        case None => throw new RuntimeException("Undefined token: " + trim)
      }
    }
  }

  def eval(instructions: String): Seq[Any] = {
    parse(instructions).map(parsed => reflection.eval(reflection.parse(parsed)))
  }

  def parse(instructions: String): Seq[String] = {
    var enrichments = Seq.empty[String]
    var current = ""
    var quote = false
    var escape = false
    var newStatement = true
    var token = ""
    var level = 0
    for (c <- instructions if c != ' ') {
      if (quote) {
        current += c
        c match {
          case '\\' => escape = true
          case '"' if !escape => quote = false
          case _ => escape = false
        }
      } else {
        if (level == 0 && (c == ',' || c == ';')) {
          if (token.nonEmpty) current += packToken(token)
          enrichments :+= current
          token = ""
          current = ""
          newStatement = true
        } else {
          var inToken = newStatement || token.length > 0
          if (inToken) {
            val s = c.toString
            if (newStatement && s.matches(tokenStart) || !newStatement && !s.matches(tokenEnd)) {
              token += c
            } else {
              current += packToken(token)
              token = ""
              inToken = false
            }
          }
          newStatement = false
          if (!inToken) {
            current += c
            c match {
              case '(' | '{' =>
                level += 1
                newStatement = true
              case ')' | '}' =>
                level -= 1
              case ',' => newStatement = true
              case '"' => quote = true
              case _ =>
            }
          }
        }
      }
    }

    if (token.nonEmpty) current += packToken(token)
    enrichments :+= current
    enrichments = enrichments.map(_.trim).filter(_.nonEmpty)
    enrichments
  }

}
