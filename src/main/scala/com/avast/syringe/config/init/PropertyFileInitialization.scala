package com.avast.syringe.config.init

import java.lang.reflect.Method
import java.util.Properties
import org.slf4j.LoggerFactory

/**
 * User: vacata
 * Date: 12/4/13
 * Time: 11:18 PM
 *
 * Initialize values of given configuration trait from `.property` file.
 */
trait PropertyFileInitialization extends ExternalInitialization with ConfigurationReader with StringConversion {

  private val logger = LoggerFactory.getLogger(classOf[PropertyFileInitialization])

  private lazy val properties: Properties = {
    val reader = configurationRead match {
      case Some(r) => r
      case None => throw new IllegalStateException("Configuration hasn't been provided!")
      case null => throw new IllegalStateException("Illegal usage of configuration reader that had already been disabled!")
    }
    val properties = new Properties()
    properties.load(reader)
    properties
  }

  def resolve(method: Method, args: Array[AnyRef]): AnyRef = {
    if (!properties.containsKey(method.getName)) {
      throw new IllegalStateException("Unable to find property %s.".format(method.getName))
    }
    val raw = properties.getProperty(method.getName)
    logger.debug("Converting {} to type {}.", Array(raw, method.getReturnType):_*)
    val result = convert(raw, method.getReturnType)
    result
  }
}
