package com.avast.syringe.config.perspective

import java.lang.reflect.InvocationHandler

/**
 * This builder is able to decorate instances. It also recognizes cyclic referencing and tries to cope with it.
 * <p/>
 * User: slajchrt
 * Date: 12/2/12
 * Time: 6:08 PM
 */
trait DecoratingBuilder[+T] extends Builder[T] {

  private[this] var workedOutInstance: Option[T] = None
  private[this] var workedOutDecoratedInstance: Option[_] = None
  private[this] var returnUnfinished = false

  /**
   * By calling this method the user gives permission to use the unfinished (worked-out, not-fully initialized)
   * instance. The instance cannot be fully initialized because of the detected cycle in referencing builders.
   */
  def circularOk: this.type = {
    returnUnfinished = true
    this
  }

  def isCircularOk = returnUnfinished

  def circular(isOk: Boolean): this.type = {
    returnUnfinished = isOk
    this
  }

  /**
   * Because of the possibility of cyclic references we have to solve this quite complex initialization.
   */
  def build[D >: T]: D = workedOutInstance match {
    case Some(inst) =>
      // Now we are in the second call, a cycle found
      if (returnUnfinished) {
        // The config user permitted to use the unfinished (the worked out) instance.
        // The worked out instance must be decorated before it is returned
        val decorated: D = delegateInstance2(decorateInstance(inst))
        // Store the decorated instance so that the first call can recognize it is already decorated
        workedOutDecoratedInstance = Some(decorated)
        decorated
      } else throw new InstanceUnderConstructionException(this, inst)
    case None => {
      // Now we are in the first call
      try {
        val inst = getInstanceClass.newInstance().asInstanceOf[T]
        // store the unfinished instance for the possible second call to recognize the cycle
        workedOutInstance = Some(inst)
        initializeInstance(inst)

        // Test if the second call already decorated the instance
        workedOutDecoratedInstance match {
          case Some(decorated) => {
            // The decorated instance is ready from the second call
            workedOutDecoratedInstance = None
            decorated.asInstanceOf[D]
          }
          case None =>
            // No decoration happened yet, so decorate
            delegateInstance2(decorateInstance(inst))
        }

      }
      finally {
        // Clear the worked out instance. It is valid only while building
        workedOutInstance = None
      }
    }
  }

  protected def decorateInstance[D >: T](firstDecorated: D): D = firstDecorated

  protected def delegateInstance[D >: T](origDelegated: D): D = origDelegated

  protected def delegateInstance2[D >: T](origDelegated: D): D = origDelegated

  protected def initializeInstance[D >: T](instance: D): D = instance

}