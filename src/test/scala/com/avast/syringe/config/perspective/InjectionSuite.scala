package com.avast.syringe.config.perspective

import com.avast.syringe.config.internal.{InjectableProperty, ConfigClassAnalyzer}
import org.scalatest.{FlatSpec, BeforeAndAfter}
import com.google.common.collect.Lists
import java.util.ArrayList
import com.avast.syringe.config.PropertyValueConverter
import java.lang.reflect.InvocationHandler
import com.avast.syringe.aop.Interceptor

/**
 * User: slajchrt
 * Date: 6/6/12
 * Time: 11:10 AM
 */

//@RunWith(classOf[JUnitRunner])
class InjectionSuite extends FlatSpec with BeforeAndAfter {

  var propMap: java.util.Map[String, InjectableProperty] = _
  var decorPropMap: java.util.Map[String, InjectableProperty] = _

  before {
    propMap = ConfigClassAnalyzer.toMap(classOf[SampleA])
    decorPropMap = ConfigClassAnalyzer.toMap(classOf[DecoratorA])
  }

  behavior of "ScalarInjection"

  it must "inject scalar value to a scalar property" in {
    expect(10) {
      val inj = new ScalarInjection(propMap.get("iProp"), List())
      val inst = new SampleA
      inj.inject(inst, List(10))
      inst.getiProp()
    }
  }

  it must "inject a reference to a scalar property" in {
    val refValue = new Runnable {
      def run() {}
    }

    expect(refValue) {
      val inj = new ScalarInjection(propMap.get("rProp"), List())
      val inst = new SampleA

      val refBuilder = new Builder[Runnable](classOf[Runnable]) {

        def makeClone(cloneName: String) = sys.error("Not supported")

        /**
         * Always creates a new instance
         * @return a new instance of instanceClass or a decorated one implementing D
         */
        def build[D >: Runnable] = refValue

        def decorateWith[D >: Runnable](decorator: => Builder[D]) = null

        def delegateWith[D <: Interceptor[_]](provider: Delegation[D]) = null

        def addPropertyResolver(resolver: PropertyResolver) = null

        def setValueConverter(converter: PropertyValueConverter) = null

        def getPropertyValueConverter = null

        def getInstanceName = null

        def getModule = null

        def syringeAllowMultiInjection = null
      }
      inj.inject(inst, List(refBuilder))

      inst.getrProp()
    }
  }

  it must "throw an exception when injecting more values to a scalar property" in {
    intercept[AssertionError] {
      val inj = new ScalarInjection(propMap.get("iProp"), List())
      val inst = new SampleA
      inj.inject(inst, List(10, 20))
    }
  }

  it must "throw an exception when injecting no value to a mandatory scalar property" in {
    intercept[AssertionError] {
      val inj = new ScalarInjection(propMap.get("sProp"), List())
      val inst = new SampleA
      inj.inject(inst, List())
    }
  }

  it must "inject a delegate reference taken from the context resolver to a delegate property" in {
    val inst = new SampleA
    val decor = new DecoratorA
    val inj = new ScalarInjection(decorPropMap.get("delegate"), List(new PropertyResolver {
      def getPropertyValue(instance: Any, property: InjectableProperty) = if (property.isDelegate)
        inst
      else throw new NoSuchFieldException()

      def hasPropertyValue(instance: Any, property: InjectableProperty) = property.isDelegate
    }))

    inj.inject(decor, List() /* no explicit value for the delegate, it is taken from the resolver */)

    assert(decor.delegate == inst)
  }

  behavior of "ArrayInjection"

  it must "inject an array value to an array property" in {
    val inj = new ArrayInjection(propMap.get("aProp"), List())
    val inst = new SampleA
    val list: List[String] = List("abc", "def")
    inj.inject(inst, list)
    assert(inst.getaProp() != null)
    assert(inst.getaProp().zip(list).forall(p => p._1 == p._2))
  }

  it must "inject an array of references to a reference array property" in {
    val inj = new ArrayInjection(propMap.get("arProp"), List())
    val inst = new SampleA
    val r1 = new Runnable {
      def run() {}
    }
    val r2 = new Runnable {
      def run() {}
    }
    val list: List[Runnable] = List(r1, r2)
    inj.inject(inst, list)
    assert(inst.getARProp != null)
    assert(inst.getARProp.zip(list).forall(p => p._1 == p._2))
  }

  it must "throw an exception when injecting an array of incompatible type" in {
    val e = intercept[IllegalArgumentException] {
      val inj = new ArrayInjection(propMap.get("aProp"), List())
      val inst = new SampleA
      val list: List[Int] = List(1, 2)
      inj.inject(inst, list)
    }
    assert(e.getMessage.contains("type mismatch"))
  }

  behavior of "CollectionInjection"

  it must "inject a list to an unitialized list property" in {
    val inj = new CollectionInjection(propMap.get("lProp"), List())
    val inst = new SampleA
    val list: List[String] = List("abc", "def")
    inj.inject(inst, list)
    assert(inst.getlProp() != null)
    assert(inst.getlProp().toArray.zip(list).forall(p => p._1 == p._2))
  }

  it must "inject a list of references to a reference collection property" in {
    val inj = new CollectionInjection(propMap.get("lrProp"), List())
    val inst = new SampleA
    val r1 = new Runnable {
      def run() {}
    }
    val r2 = new Runnable {
      def run() {}
    }
    val list: List[Runnable] = List(r1, r2)
    inj.inject(inst, list)
    assert(inst.getLRProp != null)
    assert(inst.getLRProp.toArray.zip(list).forall(p => p._1 == p._2))
  }

  it must "preserve the existing list in the property and inject values to it" in {
    val inj = new CollectionInjection(propMap.get("lProp"), List())
    val inst = new SampleA
    val listValue: ArrayList[String] = Lists.newArrayList()
    inst.setlProp(listValue)
    val list: List[String] = List("abc", "def")
    inj.inject(inst, list)
    assert(inst.getlProp() eq listValue)
    assert(inst.getlProp() != null)
    assert(inst.getlProp().toArray.zip(list).forall(p => p._1 == p._2))
  }

  it must "throw an exception when injecting a list of incompatible values" in {
    val e = intercept[IllegalArgumentException] {
      val inj = new CollectionInjection(propMap.get("lProp"), List())
      val inst = new SampleA
      val list: List[Int] = List(1, 2)
      inj.inject(inst, list)
    }
    assert(e.getMessage.contains("type mismatch"))
  }

  behavior of "MapInjection"

  it must "inject a list of pairs to an unitialized map property" in {
    val inj = new MapInjection(propMap.get("mProp"), List())
    val inst = new SampleA
    val list: List[(String, Int)] = List(("a", 1), ("b", 2))
    inj.inject(inst, list)
    assert(inst.getmProp() != null)
    assert(2 == inst.getmProp().size())
    assert(1 == inst.getmProp().get("a"))
    assert(2 == inst.getmProp().get("b"))
  }

  it must "inject a list of reference pairs to a reference map property" in {
    val inj = new MapInjection(propMap.get("mrProp"), List())
    val inst = new SampleA
    val r1 = new Runnable {
      def run() {}
    }
    val r2 = new Runnable {
      def run() {}
    }
    val list: List[(Runnable, Runnable)] = List((r1, r2), (r2, r1))
    inj.inject(inst, list)
    assert(inst.getMRProp != null)
    assert(r1 == inst.getMRProp.get(r2))
    assert(r2 == inst.getMRProp.get(r1))

  }

  it must "throw an exception when injecting a list of incompatible pairs" in {
    val inj = new MapInjection(propMap.get("mProp"), List())
    val inst = new SampleA

    val list: List[(Int, String)] = List((1, "a"), (2, "b"))
    val e = intercept[IllegalArgumentException] {
      inj.inject(inst, list)
    }
    assert(e.getMessage.contains("type mismatch"))

    val list2: List[(String, String)] = List(("a", "aa"), ("b", "bb"))
    val e2 = intercept[IllegalArgumentException] {
      inj.inject(inst, list2)
    }
    assert(e2.getMessage.contains("type mismatch"))
  }

  it must "throw an exception in case the list of pairs contains a key duplicity" in {
    val inj = new MapInjection(propMap.get("mProp"), List())
    val inst = new SampleA

    val list: List[(String, Int)] = List(("a", 1), ("a", 2))
    val e = intercept[AssertionError] {
      inj.inject(inst, list)
    }
    assert(e.getMessage.contains("To many values for key"))

  }

}
