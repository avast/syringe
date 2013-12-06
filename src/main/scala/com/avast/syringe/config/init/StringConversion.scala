package com.avast.syringe.config.init

import scala.runtime.BoxesRunTime

/**
 * User: vacata
 * Date: 12/5/13
 * Time: 10:51 PM
 */
trait StringConversion {

  def convert(input: String, targetType: Class[_]) : AnyRef = {
    ClassManifest.fromClass(targetType) match {
      case m if m <:< ClassManifest.Char => BoxesRunTime.boxToCharacter(input.head)
      case m if m <:< ClassManifest.Byte => BoxesRunTime.boxToByte(input.toByte)
      case m if m <:< ClassManifest.Short => BoxesRunTime.boxToShort(input.toShort)
      case m if m <:< ClassManifest.Int => BoxesRunTime.boxToInteger(input.toInt)
      case m if m <:< ClassManifest.Long => BoxesRunTime.boxToLong(input.toLong)
      case m if m <:< ClassManifest.Float => BoxesRunTime.boxToFloat(input.toFloat)
      case m if m <:< ClassManifest.Double => BoxesRunTime.boxToDouble(input.toDouble)
      case m if m <:< ClassManifest.Boolean => BoxesRunTime.boxToBoolean(input.toBoolean)
      case m if m <:< ClassManifest.fromClass(classOf[String]) => input
      case _ => throw new UnsupportedOperationException("Unsupported output type conversion %s!".format(targetType))
    }
  }
}
