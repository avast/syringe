package com.avast.syringe.config.init

/**
 * User: vacata
 * Date: 12/5/13
 * Time: 4:05 PM
 *
 * For any given subtrait of `ImplicitInitialization`, provides the ability to treat this subtrait as instance of any
 * given trait `T`, via implicit conversion.
 */
trait ImplicitInitialization[T] extends ExternalInitialization {

  /**
   * Implicitly convert given instance of `ImplicitInitialization` to proxying initializer of type type `T`.
   * @param c Given instance of `ImplicitInitialization`.
   * @param m Implicit manifest.
   * @tparam T Target type to be converted to. Typically some subclass of `StaticInitialization`
   *      being able to provide required properties, like `PropertyFileInitialization`.
   * @return Instance of required type `T`, dynamically created via proxying.
   */
  implicit def staticInitializationConvert[T](c: ImplicitInitialization[T])(implicit m: Manifest[T]): T = {
    if (classManifest(m).erasure.isAssignableFrom(this.getClass))
      this.asInstanceOf[T]
    else
      c.initializeValues[T]
  }
}
