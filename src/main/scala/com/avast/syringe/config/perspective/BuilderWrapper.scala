package com.avast.syringe.config.perspective

import com.avast.syringe.config.PropertyValueConverter
import java.lang.reflect.InvocationHandler
import com.avast.syringe.aop.Interceptor

/**
 * User: slajchrt
 * Date: 12/5/12
 * Time: 1:02 PM
 */
class BuilderWrapper[+T](wrapped: => Builder[T]) extends Builder[T](wrapped.getInstanceClass) {

  def getModule = wrapped.getModule

  def getInstanceName = wrapped.getInstanceName

  def getPropertyValueConverter = wrapped.getPropertyValueConverter

  def build[D >: T] = wrapped.build

  //def decorateWith[D >: T](decorator: => Builder[D]) = wrapped.decorateWith(decorator)
  def decorateWith[D >: T](decorator: => Builder[D]) = sys.error("Not supported")

  def delegateWith[D <: Interceptor[_]](provider: Delegation[D]) = sys.error("Not supported")

  //def addPropertyResolver(resolver: PropertyResolver) = wrapped.addPropertyResolver(resolver)
  def addPropertyResolver(resolver: PropertyResolver) = sys.error("Not supported")

  //def setValueConverter(converter: PropertyValueConverter) = wrapped.setValueConverter(converter)
  def setValueConverter(converter: PropertyValueConverter) = sys.error("Not supported")

  //def makeClone(cloneName: String) = wrapped.makeClone(cloneName)
  def makeClone(cloneName: String) = sys.error("Not supported")
}
