package com.avast.syringe.config.perspective

import com.avast.syringe.config.PropertyValueConverter
import java.lang.reflect.InvocationHandler
import com.avast.syringe.aop.{InterceptAllPointcut, ProxyFactory, MethodPointcut, Interceptor}
import com.avast.syringe.aop.cglib.CglibProxyFactory


/**
 * User: slajchrt
 * Date: 6/6/12
 * Time: 10:40 AM
 */

abstract class Builder[+T](instanceClass: => Class[_]) {

  /**
   * Called after this builder instance is created. The main purpose is to solve the problem
   * with initialization in traits that cannot access this builder's state during the construction
   * phase since the traits are initialized before the descendants types (i.e. the main builder class too)
   * @return
   */
  final def initialize: this.type = {
    val module: Module = getModule
    if (module != null) {
      module.initializeBuilder(this)
    }

    postConstruct()

    this
  }

  def getInstanceClass = instanceClass

  /**
   * See "initialize"
   */
  protected def postConstruct() {}

  def getModule: Module

  def getInstanceName: String

  def getPropertyValueConverter: PropertyValueConverter

  def decorateWith[D >: T](decorator: => Builder[D]): this.type

  def delegateWith[D <: Interceptor[_]](provider: Delegation[D]): this.type

  def addPropertyResolver(resolver: PropertyResolver): this.type

  def setValueConverter(converter: PropertyValueConverter): this.type

  def build[D >: T]: D

//  def build[D >: T]: D = {
//    val instance: T = buildInstance
//    val module: Module = getModule
//    if (module != null) {
//      module.decorateInstance(this, decorateInstance(instance))
//    } else {
//      instance
//    }
//  }

//  /**
//   * Always creates a new instance
//   * @return a new instance of instanceClass or a decorated one implementing D
//   */
//  protected def buildInstance[D >: T]: D
//
//  protected def decorateInstance[E >: T](instance: E): E = instance

  def makeClone(cloneName: String = null): this.type
}