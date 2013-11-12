package com.avast.syringe.config.perspective

import com.avast.syringe.config.internal.InjectableProperty

/**
 * User: slajchrt
 * Date: 6/5/12
 * Time: 3:40 PM
 */

object Injection {

  def apply(property: InjectableProperty,
            resolvers: List[PropertyResolver]): Injection = {

    property match {
      case _ if property.isArray => new ArrayInjection(property, resolvers)
      case _ if property.isCollection => new CollectionInjection(property, resolvers)
      case _ if property.isMap => new MapInjection(property, resolvers)
      case _ => new ScalarInjection(property, resolvers)
    }
  }

}

abstract class Injection(prop: InjectableProperty,
                         resolvers: List[PropertyResolver]) {

  def inject[T](instance: T, values: List[Any]): T

  protected def prepareValueForInjection(instance: Any): Any = {
    instance match {
      case builder: Builder[_] => builder.build
      case _ => instance
    }
  }

  protected def findAndSetContextualValue[T](instance: T, errorMessage: => String) {
    resolvers.find(_.hasPropertyValue(instance, prop)) match {
      case None => if (errorMessage != null) assert(false, errorMessage)
      case Some(resolver) => {
        val ctxValue = resolver.getPropertyValue(instance, prop)
        prop.setValue(instance, prepareValueForInjection(ctxValue))
      }
    }
  }

}

class ScalarInjection(prop: InjectableProperty,
                      resolvers: List[PropertyResolver]) extends Injection(prop, resolvers) {
  def inject[T](instance: T, values: List[Any]) = {
    values.size match {
      case 0 => if (!prop.isOptional) {
        findAndSetContextualValue(instance, "Missing value for mandatory property " + prop.getName + " in " + instance)
      } else if (prop.isDelegate) {
        // if the property is the delegate than the resolver must provide the delegate reference
        findAndSetContextualValue(instance, "Property resolver must provide a delegate reference for property "
          + prop.getName + " in " + instance)
      } else {
        // try to find the value for the optional property in the context - do not fire an exception if not found
        findAndSetContextualValue(instance, null /* null means no exception is fired*/)
      }
      case 1 => prop.setValue(instance, prepareValueForInjection(values.head))
      case _ => assert(false, "To many values for property " + prop.getName + " in " + instance)
    }

    instance
  }
}

class ArrayInjection(prop: InjectableProperty,
                     resolvers: List[PropertyResolver]) extends Injection(prop, resolvers) {
  def inject[T](instance: T, values: List[Any]) = {
    val componentType = prop.getArrayOrCollectionComponentType
    assert(!componentType.isArray,
      String.format("Multi-dimensional array property %s is not supported. Only single-dimensional arrays are supported",
        prop.getName))

    val array = java.lang.reflect.Array.newInstance(componentType, values.size)
    prop.setValue(instance, array)

    values.foldLeft(0)((i, v) => {
      val valToInject = prepareValueForInjection(v)
      prop.setArrayElement(instance, i, valToInject)

      i + 1
    })

    instance
  }
}

class CollectionInjection(prop: InjectableProperty,
                          resolvers: List[PropertyResolver]) extends Injection(prop, resolvers) {

  def inject[T](instance: T, values: List[Any]) = {

    if (classOf[java.util.Set[_]].isAssignableFrom(prop.getType)) {
      injectCollection(classOf[java.util.HashSet[_]])
    } else if (classOf[java.util.List[_]].isAssignableFrom(prop.getType)) {
      injectCollection(classOf[java.util.ArrayList[_]])
    } else {
      injectCollection()
    }

    def injectCollection(defaultCollectionClass: Class[_] = null) {
      var currentCollection = prop.getValue(instance).asInstanceOf[java.util.Collection[Any]]

      if (currentCollection == null) {
        currentCollection = defaultCollectionClass.newInstance.asInstanceOf[java.util.Collection[Any]]
        prop.setValue(instance, currentCollection)
      }

      values.foreach(v => {
        val valToInject: Any = prepareValueForInjection(v)

        prop.addCollectionElement(instance, valToInject)
      })
    }

    instance
  }
}

class MapInjection(prop: InjectableProperty,
                   resolvers: List[PropertyResolver]) extends Injection(prop, resolvers) {

  def inject[T](instance: T, values: List[Any]) = {
    val pairs = values.asInstanceOf[List[(AnyRef, AnyRef)]]
    var map: Map[AnyRef, AnyRef] = Map.empty
    pairs.foreach(pair => {
      val key = pair._1
      assert(!map.contains(key), "To many values for key " + key + " in property " + prop.getName + " in " + instance)
      val v = pair._2
      map += (key -> v)
    })

    var currentMap = prop.getValue(instance).asInstanceOf[java.util.Map[AnyRef, AnyRef]]

    if (currentMap == null) {
      currentMap = new java.util.HashMap[AnyRef, AnyRef]()
      prop.setValue(instance, currentMap)
    }

    map.foreach(p => {
      val k = prepareValueForInjection(p._1)
      val v = prepareValueForInjection(p._2)

      prop.putMapEntry(instance, k.asInstanceOf[AnyRef], v.asInstanceOf[AnyRef])
    })

    instance
  }
}
