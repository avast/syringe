package com.avast.syringe.config.perspective

import java.io.{PrintWriter, StringWriter}


/**
 * User: slajchrt
 * Date: 6/25/12
 * Time: 6:17 PM
 */

class InjectionException(creationStackStamp: Exception, context: (String, String), cause: Throwable) extends Exception(cause) {

  var contexts: List[(String, String)] = List(context)

  override def getMessage = {
    val w = new StringWriter()
    val pw = new PrintWriter(w)
    creationStackStamp.printStackTrace(pw)
    pw.close()

    val injectionPointsStackElems = findLinkToCode()

    var injPoints: String = ""
    injectionPointsStackElems.foreach(ip => {
      injPoints += "\n    at " + ip.getClassName + "." + ip.getMethodName + "(" + ip.getFileName + ":" + ip.getLineNumber + ")"
    })

    "\n\n***Problem summary***" +
      "\nDescription:\n    " + cause.getMessage +
      "\nInjection points:" + injPoints +
      "\nContext path (builderName, property):\n    " + contexts.toString() +
      "\n\n***Builder creation stack trace***\n" +
      w.toString +
      "\n***Actual exception stack trace***"
  }

  private def findLinkToCode(): List[StackTraceElement] = {

    val trace: Array[StackTraceElement] = creationStackStamp.getStackTrace

    def findFirstMark(from: Int): Int = {
      trace.indexWhere(se => se.getMethodName == "__sm__", from)
    }

    var occurrences: List[StackTraceElement] = List()
    var from = 0
    do {
      from = findFirstMark(from)
      if (from > 0) {
        occurrences ::= trace(from - 1)
        from += 1
      }
    } while (from >= 0)
    occurrences.reverse
  }
}
