package com.avast.syringe.config.perspective

import com.avast.syringe.config.InstanceManager

/**
 * User: slajchrt
 * Date: 6/12/12
 * Time: 7:15 PM
 */

trait Module extends InstanceManager {

  def main(args: Array[String]) {}

  /**
   * Called by the builder's "initialize" method
   * @param builder
   */
  def initializeBuilder[T](builder: Builder[T]) {}

  //def decorateInstance[T](builder: Builder[T], instance: T): T = instance

  /**
   * @return the map of used builders
   */
  def getBuilders: Map[String, Builder[_]]

  /**
   * Finds the builder that created the named instance
   * @param instanceName the instance name
   * @tparam T
   * @return the used builder
   */
  def getBuilder[T](instanceName: String): Builder[T]

  def getInstance[T](instanceName: String): T = getBuilder(instanceName).build

  def load[T](instanceName: String) = sys.error("Not implemented")

  def findRefName(ref: Any): String = {
    getBuilders.find(p => {
      p._2.isInstanceOf[SingletonBuilder[_]] && p._2.asInstanceOf[SingletonBuilder[_]].isLoaded
    }) match {
      case None => null
      case Some((instName, builder)) => if (builder.build == ref) instName else null
    }
  }
}
