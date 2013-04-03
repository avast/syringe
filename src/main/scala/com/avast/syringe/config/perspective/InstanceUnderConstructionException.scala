package com.avast.syringe.config.perspective

/**
 * User: slajchrt
 * Date: 12/2/12
 * Time: 2:50 PM
 */
class InstanceUnderConstructionException[T](val builder: Builder[T], val unfinishedInstance: T) extends RuntimeException
