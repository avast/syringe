package com.avast.syringe.config.perspective

/**
 * User: slajchrt
 * Date: 12/5/12
 * Time: 1:10 PM
 */
class CircularBuilder[+T](wrapped: => DecoratingBuilder[T]) extends BuilderWrapper(wrapped) {

  override def build[D >: T] = {
    val saved = wrapped.isCircularOk
    try {
      wrapped.circular(isOk = true)
      wrapped.build
    }
    finally {
      wrapped.circular(saved)
    }
  }

}

object circular {
  def apply[T](wrapped: => DecoratingBuilder[T]) = new CircularBuilder[T](wrapped)
}