package com.avast.syringe.config.perspective

import com.avast.syringe.aop.{ProxyFactory, InterceptAllPointcut, MethodPointcut, Interceptor}
import com.avast.syringe.aop.cglib.CglibProxyFactory
import com.avast.syringe.config.perspective.Delegation._
import com.avast.syringe.config.perspective.Delegation.InterfaceListTarget
import com.avast.syringe.config.perspective.Delegation.ClassTarget
import com.avast.syringe.config.perspective.Delegation.DefaultClassTarget

/**
 * User: vacata
 * Date: 1/29/13
 * Time: 5:50 PM
 */
case class Delegation[T <: Interceptor[_]](val intercept: Builder[T],
                                           val target: Delegation.Target = DefaultClassTarget(),
                                           val pointcut: MethodPointcut = new InterceptAllPointcut,
                                           val factory: ProxyFactory = new CglibProxyFactory) {

  var interceptorFactory: () => Builder[T] = null

  def proxyTarget(clazz: Class[_]): Array[Class[_]] = {
    target match {
      case t: ClassTarget => Array(t.clazz)
      case t: InterfaceListTarget => t.interfaces.toArray
      case t: DefaultClassTarget => Array(clazz)
      case t: DefaultInterfaceListTarget => clazz.getInterfaces
      case _ => throw new UnsupportedOperationException("Unsupported target type.")
    }
  }
}

object Delegation {

  sealed trait Target

  case class ClassTarget(clazz: Class[_]) extends Target

  case class DefaultClassTarget() extends Target

  case class InterfaceListTarget(interfaces: List[Class[_]]) extends Target

  case class DefaultInterfaceListTarget() extends Target

}