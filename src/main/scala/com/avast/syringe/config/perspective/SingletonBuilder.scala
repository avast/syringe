package com.avast.syringe.config.perspective

import com.avast.syringe.SingletonProviderFactory


/**
 * User: slajchrt
 * Date: 6/12/12
 * Time: 6:42 PM
 */

trait SingletonBuilder[T] extends DecoratingBuilder[T] {

  private[this] var instance = List[T]()

  def isLoaded = !instance.isEmpty

  override def build[D >: T] = {
    this.synchronized {
      if (instance.isEmpty) {
        instance ::= super.build
      }
      instance.head
    }
  }

  override def decorateInstance[E >: T](instance: E) = {
    super.decorateInstance(makeSingletonProvider(instance))
  }

  protected def unwrap(instance: Any) = SingletonProviderFactory.unwrap(instance)

  private def makeSingletonProvider[E >: T](instance: E) =
    if (instance.isInstanceOf[com.avast.syringe.Provider[_]]) {
      val provider = instance.asInstanceOf[com.avast.syringe.Provider[_]]
      SingletonProviderFactory.createSingletonProvider(provider).asInstanceOf[E]
    } else {
      instance
    }

  def getInstance = build
}
