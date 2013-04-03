package com.avast.syringe.config.perspective

import com.avast.syringe.config.internal.Injection.ContextualPropertyResolver
import com.avast.syringe.config.PropertyValueConverter


/**
 * User: slajchrt
 * Date: 6/11/12
 * Time: 12:43 PM
 */

trait InstanceCache[T] extends Builder[T] {

//  private[this] var instanceCache = Map.empty[String, Object]
//
//  override def build(resolver: ContextualPropertyResolver, setValueConverter: PropertyValueConverter) = {
//    instanceCache.get(getInstanceName) match {
//      case Some(cached) => cached.asInstanceOf[T]
//      case None => {
//        val instance: T = build
//        instanceCache += (getInstanceName -> instance.asInstanceOf[Object])
//        instance
//      }
//    }
//  }
}
