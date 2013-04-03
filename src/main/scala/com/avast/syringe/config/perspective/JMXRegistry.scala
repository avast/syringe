package com.avast.syringe.config.perspective

import management.ManagementFactory
import com.avast.syringe.config.mbean.ConfigDynamicBean
import com.avast.syringe.config.internal.ConfigClassAnalyzer
import javax.management.ObjectName
import org.slf4j.LoggerFactory

/**
 * User: slajchrt
 * Date: 6/11/12
 * Time: 12:37 PM
 */

object JMXRegistry {
  private val mBeanServer = ManagementFactory.getPlatformMBeanServer
  private val LOGGER = LoggerFactory.getLogger(classOf[JMXRegistry[_]])
}

trait JMXRegistry[T] extends SingletonBuilder[T] {

  private[this] var namespace: String = "com.avast.syringe.instances"

  def mBeanNamespace(ns: String): this.type = {
    namespace = ns
    this
  }


  override def build[D >: T] = {
    val wasAlreadyCreated = isLoaded
    val instance: T = super.build

    if (!wasAlreadyCreated)
      registerMBean(instance)

    instance
  }

  private def registerMBean(instance: T) {
    val configProps = new ConfigClassAnalyzer(getInstanceClass, getPropertyValueConverter).getConfigProperties

    try {
      val configDynamicBean = new ConfigDynamicBean(unwrap(instance), getInstanceClass.getName, "", configProps, getModule)
      val configMBeanName = new ObjectName(namespace + ":type=" + getInstanceClass.getName + ",name=" + getInstanceName)
      JMXRegistry.mBeanServer.registerMBean(configDynamicBean, configMBeanName)
    }
    catch {
      case e: Exception => {
        JMXRegistry.LOGGER.warn("Cannot register JMX bean {}", e.getMessage)
      }
    }
  }

}
