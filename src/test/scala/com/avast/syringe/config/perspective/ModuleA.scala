package com.avast.syringe.config.perspective

import com.avast.syringe.config.perspective.TestingInterceptor1.InterceptCounter

/**
 * User: slajchrt
 * Date: 6/14/12
 * Time: 11:29 AM
 */
trait ModuleA extends SyringeModule {

  object SampleABuilder {
    type instanceType = SampleA
    val instanceClass = classOf[instanceType]
  }

  class SampleABuilder private[ModuleA]() extends SyringeBuilder[SampleABuilder.instanceType](SampleABuilder.instanceClass) {
    def set(propertyName: String, value: => Any): this.type = inject(propertyName, value)
    def sProp(value: String): this.type = inject("sProp", value)
    def iProp(value: Int): this.type = inject("iProp", value)
    def addTolrProp(value: Builder[_]): this.type = inject("lrProp", value)
  }

  def newSampleA = new SampleABuilder().initialize
  lazy val sampleA = new SampleABuilder().initialize

  class SingletonSampleABuilder private[ModuleA]() extends SyringeBuilder[SampleABuilder.instanceType](SampleABuilder.instanceClass)
  with SingletonBuilder[SampleABuilder.instanceType] {
    def set(propertyName: String, value: => Any): this.type = inject(propertyName, value)
    def sProp(value: String): this.type = inject("sProp", value)
  }

  def newSingletonSampleA = new SingletonSampleABuilder().initialize

  object DecoratorABuilder {
    type instanceType = DecoratorA
    val instanceClass = classOf[instanceType]
  }

  class DecoratorABuilder private[ModuleA]() extends SyringeBuilder[DecoratorABuilder.instanceType](DecoratorABuilder.instanceClass) {
    def set(propertyName: String, value: => Any): this.type = inject(propertyName, value)
    def delegate(value: String): this.type = inject("delegate", value)
    def id(value: Int): this.type = inject("id", value)
  }

  def newDecoratorA = new DecoratorABuilder().initialize

  class SimpleServiceBuilder private[ModuleA]() extends SyringeBuilder[SimpleServiceBuilder.instanceType](SimpleServiceBuilder.instanceClass)
  //with SingletonBuilder[SimpleServiceBuilder.instanceType] {
  {
  }

  object SimpleServiceBuilder {
    type instanceType = SimpleServiceImpl
    val instanceClass = classOf[instanceType]
  }

  def newSimpleService = new SimpleServiceBuilder().initialize

  class TestingInterceptor1Builder private[ModuleA]() extends SyringeBuilder[TestingInterceptor1Builder.instanceType](TestingInterceptor1Builder.instanceClass)
  //with SingletonBuilder[TestingInterceptor1Builder.instanceType] {
  {
    def set(propertyName: String, value: => Any): this.type = inject(propertyName, value)
    def rawTarget(value: String): this.type = inject("rawTarget", value)
    def interceptCounter(value: InterceptCounter): this.type = inject("interceptCounter", value)
  }

  object TestingInterceptor1Builder {
    type instanceType = TestingInterceptor1
    val instanceClass = classOf[instanceType]
  }

  def newTestingInterceptor1 = new TestingInterceptor1Builder().initialize
}
