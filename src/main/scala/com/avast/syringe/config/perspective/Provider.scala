package com.avast.syringe.config.perspective

/**
 * User: slajchrt
 * Date: 6/14/12
 * Time: 9:32 AM
 */

trait Provider[+T] {

  def getInstance: T

}
