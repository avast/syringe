package com.avast.syringe.config.perspective

import com.avast.syringe.config.ConfigProperty
import scala.annotation.target.field

/** Provides utilities to better integrate Scala code. */
object Integration {

  /** Alias for [[com.avast.syringe.config.ConfigProperty]] with [[scala.annotation.target.field]]] annotation.
    *
    * {{{
    * class X (@Inject val y: Int)
    * }}}
    */
  type Inject = ConfigProperty @field

}
