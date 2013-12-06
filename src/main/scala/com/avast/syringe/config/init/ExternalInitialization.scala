package com.avast.syringe.config.init

import java.lang.reflect.{Proxy, Method, InvocationHandler}
import org.slf4j.LoggerFactory
import scala.collection.mutable

/**
 * User: vacata
 * Date: 12/4/13
 * Time: 10:29 PM
 *
 * Base trait for all classes supporting external initialization. Initialize "externally" means loading some values from
 * a resource like static file (e.g. `.properties` or `.xml`) to support configuration changes without a need of
 * recompiling Syringe perspectives.
 */
trait ExternalInitialization {

  private val logger = LoggerFactory.getLogger(classOf[ExternalInitialization])

  private val registry: mutable.Map[ClassManifest[_], AnyRef] = new mutable.HashMap[ClassManifest[_], AnyRef]()

  def resolve(method: Method, args: Array[AnyRef]): AnyRef

  def initializeValues[T : ClassManifest]: T = {
    val theClass = classManifest[T].erasure.asInstanceOf[Class[T]]
    if (!registry.contains(classManifest[T])) {
      registry.synchronized {
      if (!registry.contains(classManifest[T])) {
        logger.info("Creating new proxy for type {}.", classManifest[T])
        registry.put(classManifest[T],
          Proxy.newProxyInstance(
            getClass.getClassLoader, Array(theClass), new InvocationHandler {
              def invoke(proxy: scala.Any, method: Method, args: Array[AnyRef]): AnyRef = {
                logger.debug("Trying to statically resolve method {}.", method)
                resolve(method, args)
              }
            })
          )
        }
      }
    }
    registry(classManifest[T]).asInstanceOf[T]
  }
}
