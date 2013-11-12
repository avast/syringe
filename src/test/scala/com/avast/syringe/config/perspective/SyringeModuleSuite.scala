package com.avast.syringe.config.perspective

import org.scalatest.{BeforeAndAfter, FlatSpec}
import com.avast.syringe.config.PropertyValueConverter
import java.util.List
import com.avast.syringe.config.internal.InjectableProperty
import com.avast.syringe.config.perspective.TestingInterceptor1.InterceptCounter
import collection.immutable

/**
 * User: slajchrt
 * Date: 6/14/12
 * Time: 11:28 AM
 */

class SyringeModuleSuite extends FlatSpec with BeforeAndAfter {

  object App extends ModuleA

  import App._

  behavior of "SyringeBuilder"

  before {
    SampleA.initSequence.set(0)
  }

  it must "return distinct instances if called more than once" in {
    val aBuilder = App.newSampleA
      .sProp("abc")
    val s1 = aBuilder.build
    val s2 = aBuilder.build

    assert(s1 ne s2)
  }

  it must "decorate by one decorator and call post-construct methods in the proper order" in {
    val r: Runnable = App.newSampleA
      .sProp("abc")
      .decorateWith(App.newDecoratorA)
      .build

    assert(r.isInstanceOf[DecoratorA])
    assert(r.asInstanceOf[DecoratorA].delegate.isInstanceOf[SampleA])

    assert(r.asInstanceOf[DecoratorA].delegate.asInstanceOf[SampleA].initOrder == 1)
    assert(r.asInstanceOf[DecoratorA].initOrder == 2)
  }

  it must "decorate by two different decorator instances and call post-construct methods in the proper order" in {
    val r: Runnable = App.newSampleA
      .sProp("abc")
      .decorateWith(App.newDecoratorA
      .id(1))
      .decorateWith(App.newDecoratorA
      .id(2))
      .build

    assert(r.isInstanceOf[DecoratorA])
    assert(r.asInstanceOf[DecoratorA].id == 2)
    assert(r.asInstanceOf[DecoratorA].delegate.isInstanceOf[DecoratorA])
    assert(r.asInstanceOf[DecoratorA].delegate.asInstanceOf[DecoratorA].id == 1)
    assert(r.asInstanceOf[DecoratorA].delegate.asInstanceOf[DecoratorA].delegate.isInstanceOf[SampleA])

    assert(r.asInstanceOf[DecoratorA].delegate.asInstanceOf[DecoratorA].delegate.asInstanceOf[SampleA].initOrder == 1)
    assert(r.asInstanceOf[DecoratorA].delegate.asInstanceOf[DecoratorA].initOrder == 2)
    assert(r.asInstanceOf[DecoratorA].initOrder == 3)
  }

  it must "return always the same instance if decorated with SingletonBuilder trait" in {
    val aBuilder = App.newSingletonSampleA
      .sProp("abc")
    val s1 = aBuilder.getInstance
    val s2 = aBuilder.getInstance

    assert(s1 eq s2)
  }

  it must "compose multiple property value converters" in {
    val conv1 = new PropertyValueConverter {
      def convertTo[T](prop: InjectableProperty, instance: Any, targetPropertyClass: Class[T], sourceValue: Any) =
        if ("abc" == sourceValue)
          "xyz".asInstanceOf[T]
        else sourceValue.asInstanceOf[T]
    }

    val conv2 = new PropertyValueConverter {
      def convertTo[T](prop: InjectableProperty, instance: Any, targetPropertyClass: Class[T], sourceValue: Any) =
        if (123 == sourceValue)
          456.asInstanceOf[T]
        else sourceValue.asInstanceOf[T]
    }

    val aBuilder = App.newSampleA
      .setValueConverter(conv1)
      .setValueConverter(conv2)
      .sProp("abc")
      .iProp(123)

    val instance = aBuilder.build
    assert(instance.getsProp() == "xyz")
    assert(instance.getiProp() == 456)
  }

  it must "reproduce a builder template" in {
    val sample = App.newSampleA
      .sProp("root")
      .addTolrProp(reproduce(("a", 1), ("b", 2), ("c", 3)) {
      x => newSampleA
        .sProp(x._1)
        .iProp(x._2)
    }).build

    val lrProp: List[Runnable] = sample.getLRProp
    assert(3 == lrProp.size)
    assert(lrProp.get(0).asInstanceOf[SampleA].getsProp() == "a")
    assert(lrProp.get(0).asInstanceOf[SampleA].getiProp() == 1)
    assert(lrProp.get(1).asInstanceOf[SampleA].getsProp() == "b")
    assert(lrProp.get(1).asInstanceOf[SampleA].getiProp() == 2)
    assert(lrProp.get(2).asInstanceOf[SampleA].getsProp() == "c")
    assert(lrProp.get(2).asInstanceOf[SampleA].getiProp() == 3)
  }

  it must "inject properties by values provided by the installed property resolver" in {

    object M extends ModuleA {
      addResolver((prop) => {
        // sProp is a mandatory property delivered by this resolver
        case _: &[String] if prop.hasTag("s") => "Hello"
        // iProp is a mandatory property delivered by this resolver
        case _: &[Int] if prop.hasTag("i") => 10
      })
    }

    val aBuilder = M.newSingletonSampleA
    val s1 = aBuilder.getInstance

    assert(s1.getsProp() eq "Hello")
    assert(s1.getiProp() == 10)
  }

  implicit def toIntRef(i: Int) = i.asInstanceOf[java.lang.Integer]

  it must "inject properties by values provided by more installed property resolvers" in {

    object M extends ModuleA {
      addResolver((prop) => {
        // sProp is a mandatory property delivered by this resolver
        case _: &[String] if prop.hasTag("s") => "Hello"
      })
      addResolver((prop) => {
        // iProp is a mandatory property delivered by this resolver
        case _: &[Int] if prop.hasTag("i") => 10
      })
    }

    val aBuilder = M.newSingletonSampleA
    val s1 = aBuilder.getInstance

    assert(s1.getsProp() eq "Hello")
    assert(s1.getiProp() == 10)
  }

  it must "convert values by means of the installed property sconverter" in {

    object M extends ModuleA {
      addConverter((prop, sourceValue) => {
        // sProp is a mandatory property delivered by this resolver
        case _: &[String] if prop.hasTag("s") => sourceValue
        // iProp is a mandatory property delivered by this resolver
        case _: &[Int] if prop.hasTag("i") => sourceValue.toString.toInt
      })
    }

    val aBuilder = M.newSampleA
      .set("iProp", "10")
      .set("sProp", "Hi")
    val s1 = aBuilder.build

    assert(s1.getsProp() eq "Hi")
    assert(s1.getiProp() == 10)
  }

  it must "merge values and default values" in {
    val aBuilder = App.newSampleA
      .addDefaultValue("sProp", "xyz")
      .addDefaultValue("iProp", 1)
      .iProp(2)

    val merged: List[(String, Any)] = aBuilder.mergeValuesAndDefaultValues()
    assert(merged.size() == 2)
    assert(merged.get(0)._1 == "sProp" && merged.get(0)._2 == "xyz")
    assert(merged.get(1)._1 == "iProp" && merged.get(1)._2 == 2)
  }

  it must "create a clone with the origin's prop values as the default props in the clone" in {
    val aBuilder = App.newSampleA
      .sProp("abc")
      .iProp(1)
    val aClone = aBuilder.makeClone("aClone")
    aClone.iProp(2)

    val c = aClone.build

    assert(c.getsProp() == "abc")
    assert(c.getiProp() == 2)
  }

  it must "call the interceptor" in {
    val interceptCounter = new InterceptCounter

    val builder = App.newSimpleService
      .delegateWith(Delegation(App.newTestingInterceptor1.interceptCounter(interceptCounter)))
    val simpleService = builder.build

    simpleService.doSomething()

    assert(interceptCounter.getBeforeCount == 1)
    assert(interceptCounter.getAfterCount == 1)
  }

  it must "call the interceptor using interface based interception" in {
    val interceptCounter = new InterceptCounter
    val builder = App.newSimpleService
      .delegateWith(Delegation(
        intercept = App.newTestingInterceptor1.interceptCounter(interceptCounter),
        target = Delegation.InterfaceListTarget(immutable.List(classOf[SimpleService]))
    ))
    val simpleService: SimpleService = builder.build

    simpleService.doSomething()

    assert(interceptCounter.getBeforeCount == 1)
    assert(interceptCounter.getAfterCount == 1)
  }

  it must "call the interceptor twice" in {
    val interceptCounter = new InterceptCounter
    val testingInterception = Delegation(App.newTestingInterceptor1.interceptCounter(interceptCounter))

    val builder = App.newSimpleService
      .delegateWith(testingInterception)
      .delegateWith(testingInterception)
    val simpleService = builder.build

    simpleService.doSomething()

    assert(interceptCounter.getBeforeCount == 2)
    assert(interceptCounter.getAfterCount == 2)
  }

  it must "Throw InjectionException while trying to do multiple injections of the same property" in {
    intercept[InjectionException]{
      val r = App.newSampleA
        .sProp("abc")
        .sProp("def")
        .build
    }
  }

  it must "Allow multiple injections of the same property when requested" in {
    val r = App.newSampleA
      .sProp("abc")
      .sProp("def")
      .syringeAllowMultiInjection
      .build

    assert(r.getsProp() === "def")
  }
}
