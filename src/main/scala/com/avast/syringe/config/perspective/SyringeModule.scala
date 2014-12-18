package com.avast.syringe.config.perspective

import com.avast.syringe.config.PropertyValueConverter
import javax.annotation.PostConstruct
import com.avast.syringe.config.internal.{InjectableProperty, ConfigClassAnalyzer}
import com.google.common.collect.Lists
import java.lang.reflect.{Proxy, InvocationHandler}
import com.avast.syringe.aop.Interceptor
import org.slf4j.LoggerFactory

/**
 * The base class for generated modules
 * <p/>
 * User: slajchrt
 * Date: 6/4/12
 * Time: 6:34 PM
 */

class SyringeModule extends Module {

  private val LOGGER = LoggerFactory.getLogger(classOf[SyringeModule])

  protected var builders = Map.empty[String, Builder[_]]

  def getBuilders = builders

  type =>?[-A, +B] = PartialFunction[A, B]
  type RF = Array[_] =>? Builder[_]
  type Res = (InjectableProperty) => RF
  type CF = Any =>? Any
  type Conv = (InjectableProperty, Any) => CF
  type &[T] = Array[T]

  private[this] var resolvers: List[Res] = Nil
  private[this] var converters: List[Conv] = Nil

  def addResolver(r: Res) {
    resolvers ::= r
  }

  def addConverter(c: Conv) {
    converters ::= c
  }

  private def combineResolvers(prop: InjectableProperty): RF = {
    val partialResolvers = resolvers.map(r => r(prop))
    val combined = partialResolvers.tail.foldLeft(partialResolvers.head) {
      (folded, f) => folded orElse f
    }
    combined
  }

  private def combineConverters(prop: InjectableProperty, sourceValue: Any): CF = {
    val partialConverters = converters.map(c => c(prop, sourceValue))
    val combined = partialConverters.tail.foldLeft(partialConverters.head) {
      (folded, f) => folded orElse f
    }
    combined
  }

  /**
   * Called by the builder's "initialize" method
   * @param builder
   */
  override def initializeBuilder[T](builder: Builder[T]) {

    super.initializeBuilder(builder)

    if (!resolvers.isEmpty) {
      installResolversToBuilder(builder)
    }

    if (!converters.isEmpty) {
      installConvertersToBuilder(builder)
    }
  }

  private def createDummyVal(property: InjectableProperty): Array[_] =
    java.lang.reflect.Array.newInstance(property.getType, 0).asInstanceOf[Array[_]]

  private def installResolversToBuilder[T](builder: Builder[T]) {
    builder.addPropertyResolver(new PropertyResolver {
      def getPropertyValue(instance: Any, property: InjectableProperty) = {
        val combinedResolver: RF = combineResolvers(property)
        val dummyVal = createDummyVal(property)
        val builder: Builder[_] = combinedResolver(dummyVal)
        builder.build.asInstanceOf[AnyRef]
      }

      def hasPropertyValue(instance: Any, property: InjectableProperty) = {
        val combinedResolver: RF = combineResolvers(property)
        val dummyVal = createDummyVal(property)
        combinedResolver.isDefinedAt(dummyVal)
      }
    })
  }

  private def installConvertersToBuilder[T](builder: Builder[T]) {
    builder.setValueConverter(new PropertyValueConverter {
      def convertTo[T](prop: InjectableProperty, instance: Any, targetPropertyClass: Class[T], sourceValue: Any) = {
        val combinedConverter: CF = combineConverters(prop, sourceValue)
        val dummyVal = createDummyVal(prop)
        combinedConverter.lift(dummyVal) match {
          case None => sourceValue.asInstanceOf[T]
          case Some(v) => v.asInstanceOf[T]
        }
      }
    })
  }

  def getBuilder[T](instanceName: String): Builder[T] = {
    builders.get(instanceName) match {
      case None => sys.error("Builder for instance " + instanceName + " not found")
      case Some(builder) => builder.asInstanceOf[SyringeBuilder[T]]
    }
  }

  type ReproductionType[X, V] = (List[X], (X) => Builder[V])

  def reproduce[X, V](args: X*)(reproducer: (X) => Builder[V]): ReproductionType[X, V] =
    reproduce(List(args: _*))(reproducer)

  def reproduce[X, V](args: List[X])(reproducer: (X) => Builder[V]): ReproductionType[X, V] = (args.reverse, reproducer)

  def decorateInstance[T](builder: Builder[T], instance: T) = instance

  /**
   * Stack marker
   */
  protected def __sm__[X](value: => X): X = value

  protected def __sm__[X](builder: SyringeBuilder[_], value: => X): X = value

  /**
   * Strips all decorations from the decorated object.
   * @param decorated the decorated object
   * @tparam A
   * @return the stripped object
   */
  def strip[A](decorated: A): A = ConfigClassAnalyzer.stripDeep(decorated).asInstanceOf[A]

  class SyringeBuilder[+T](instanceClass: Class[_], defaultInstanceName: String)
    extends Builder[T](instanceClass) with DecoratingBuilder[T] with Cloneable {

    private var values = List[(String, Any)]()
    private var defaultValues = List[(String, Any)]()
    private var decorators = List[() => Builder[_]]()
    private var delegateProxies = List[ProxyDef]()
    private var delegationProviders = List[Delegation[_]]()
    private var instanceName: String = defaultInstanceName
    private var resolvers = List[PropertyResolver]()
    private var converter: PropertyValueConverter = null
    private var multipleInjectionsAllowed = false

    val creationStackStamp = new Exception()

    def this(instanceClass: Class[_]) = this(instanceClass, instanceClass.getSimpleName)

    def mergeValuesAndDefaultValues(): List[(String, Any)] = {
      val valNames = values.map(_._1).toSet
      val defValNames = defaultValues.map(_._1).toSet
      // the default property names not included in the values
      val defValNamesNoVal = defValNames.diff(valNames)

      val defValsPerName = defValNamesNoVal.flatMap(defPropName => {
        defaultValues.filter(defProp => defProp._1 == defPropName)
      })

      // concatenate the exclusive default properties with the values
      values ::: defValsPerName.toList
    }

    def addDefaultValue(propName: String, propValue: Any): this.type = {
      defaultValues ::=(propName, propValue)
      this
    }

    def makeClone(cloneName: String = null): this.type = {

      val c = clone().asInstanceOf[this.type]
      c.values = Nil
      c.defaultValues = mergeValuesAndDefaultValues()
      c.decorators = decorators.toList
      c.instanceName = cloneName
      c.converter = converter
      c.resolvers = resolvers.toList
      c
    }

    /**
     * See "initialize"
     */
    override protected def postConstruct() {
      super.postConstruct()
      guessInstanceName()
    }


    def guessInstanceName() {
      val trace: Array[StackTraceElement] = new Exception().getStackTrace
      if (trace(3).getMethodName.startsWith("new")) {
        instanceName(trace(3).getMethodName.substring(3))
      } else {
        instanceName(trace(1).getMethodName.substring(3))
      }

      //println("SyringeBuilder:guessInstanceName=" + instanceName)
    }

    def getModule = SyringeModule.this

    def getPropertyValueConverter = converter

    def inject[B](propertyName: String, value: => Any): B = {
      val v = __sm__(this, value)

      v match {
        // try to unwrap a builders reproduction
        case repWrapper: ReferenceWrapper[_] => repWrapper.ref match {
          case rep: ReproductionType[_, _] @unchecked => {
            val args = rep._1
            val reproducer = rep._2
            // reproduce the builders
            args.foreach(arg => {
              val builder = reproducer(arg)
              values ::=(propertyName, builder)
            })
          }
          case _ => values ::=(propertyName, value)
        }
        case _ => {
          values ::=(propertyName, v)
        }
      }

      this.asInstanceOf[B]
    }

    def inject[B](propertyName: String, key: => Any, value: => Any): B = {
      val k = __sm__(this, key)
      val v = __sm__(this, value)

      values ::=(propertyName, (k, v))
      this.asInstanceOf[B]
    }

    def decorateWith[D >: T](decorator: => Builder[D]): this.type = {
      val f = () => __sm__(this, decorator)
      decorators ::= f
      this
    }

    def delegateWith[D <: Interceptor[_]](provider: Delegation[D]) : this.type = {
      val f = () => __sm__(this, provider.intercept)
      provider.interceptorFactory = f
      delegationProviders ::= provider
      this
    }

    def instanceName(name: String): this.type = {
      this.instanceName = name
      this
    }

    def getInstanceName = instanceName

    def addPropertyResolver(resolver: PropertyResolver): this.type = {
      this.resolvers ::= resolver
      this
    }

    def setValueConverter(conv: PropertyValueConverter): this.type = {
      this.converter = if (this.converter == null)
        conv
      else
        composeConverters(this.converter, conv)
      this
    }

    def get(propName: String): Option[Any] = {
      getPropertyValues(propName) match {
        case Nil => None
        case head :: Nil => Some(head)
        case _ => sys.error("Too many values for property " + propName)
      }
    }

    def apply(propName: String): Any = {
      getPropertyValues(propName) match {
        case Nil => sys.error("No value for property " + propName)
        case head :: Nil => head
        case _ => sys.error("Too many values for property " + propName)
      }
    }

    def ref[R](refPropName: String): R = {
      apply(refPropName) match {
        case a: Builder[_] => a.asInstanceOf[R]
        case _ => sys.error("Property " + refPropName + " is not a reference (builder)")
      }
    }

    def getPropertyValues(propName: String) = {
      values.filter(_._1 == propName).map(_._2) match {
        case Nil => defaultValues.filter(_._1 == propName).map(_._2)
        case vs if multipleInjectionsAllowed => vs.take(1)
        case vs => vs
      }
    }

    override protected def initializeInstance[D >: T](instance: D): D = {
      builders += (instanceName -> this)

      val propIter = new ConfigClassAnalyzer(instanceClass, converter).getConfigProperties.iterator()
      while (propIter.hasNext) {
        val property = propIter.next()
        val propValues = getPropertyValues(property.getName)

        val injection: Injection = Injection(property, resolvers)
        try {
          injection.inject(instance, propValues)
        }
        catch {
          case injErr: InjectionException => {
            injErr.contexts ::=(getInstanceName, property.getName)
            throw injErr
          }
          case err: Throwable => {
            throw new InjectionException(creationStackStamp, (getInstanceName, property.getName), err)
          }
        }
      }

      notifyPostConstruct(instance)

      instance
    }

    override def decorateInstance[D >: T](firstDecorated: D): D = {
      var decorated: D = SyringeModule.this.decorateInstance(this, super.decorateInstance(firstDecorated))

      decorators.reverse.foreach(decoratorBuilderFactory => {
        val decoratorBuilder = decoratorBuilderFactory()
        decoratorBuilder.addPropertyResolver(new DecoratorResolver(decorated.asInstanceOf[AnyRef]))
        decorated = decoratorBuilder.build.asInstanceOf[D]
      })

      decorated
    }

    override def delegateInstance[D >: T](origDelegated: D): D = {
      var delegated: D = origDelegated

      delegateProxies.reverse.foreach(pd => {
        val invocationHandlerBuilder = pd.factory()
        invocationHandlerBuilder.addPropertyResolver(new DecoratorResolver(delegated.asInstanceOf[AnyRef]))
        val invocationHandler = invocationHandlerBuilder.build.asInstanceOf[InvocationHandler]
        delegated = Proxy.newProxyInstance(this.getClass.getClassLoader, pd.interfaces, invocationHandler).asInstanceOf[D]
      })
      delegated
    }

    override protected def delegateInstance2[D >: T](origDelegated: D) = {
      var delegated: D = origDelegated;

      delegationProviders.reverse.foreach(p => {
        try {
          val interceptorBuilder: Builder[_] = p.interceptorFactory();
          interceptorBuilder.addPropertyResolver(new DecoratorResolver(delegated.asInstanceOf[AnyRef]))
          val proxyTarget = p.proxyTarget(getInstanceClass)
          val interceptor = interceptorBuilder.build.asInstanceOf[Interceptor[_]]
          if (!p.factory.canCreate(proxyTarget)) {
            throw new RuntimeException("Invalid usage of proxy factory!")
          }
          val proxy = p.factory.createProxy(interceptor, proxyTarget, p.pointcut).asInstanceOf[D]
          if (proxy != null) {
            delegated = proxy
          }
        }
        catch {
          case e : Exception => LOGGER.error("Unable to apply delegation %s".format(p), e)
        }
      })
      delegated
    }


    /**
     * Allow multiple injections of single property (mainly for testing purposes).
     * In case of multiple injections, the last one will be applied.
     */
    def syringeAllowMultiInjection = {
      multipleInjectionsAllowed = true
      this
    }

    private def notifyPostConstruct[D >: T](instance: D) {
      val postConstMethod = ConfigClassAnalyzer.findAnnotatedMethod(classOf[PostConstruct], instanceClass)
      if (postConstMethod != null) {
        postConstMethod.invoke(instance)
      }
    }

    private def composeConverters(first: PropertyValueConverter, second: PropertyValueConverter): PropertyValueConverter = {
      new PropertyValueConverter {
        def convertTo[T](prop: InjectableProperty, instance: Any, targetPropertyClass: Class[T], sourceValue: Any): T = {
          val firstRes = first.convertTo(prop, instance, targetPropertyClass, sourceValue)
          if (firstRes != sourceValue)
            firstRes
          else
            second.convertTo(prop, instance, targetPropertyClass, sourceValue)
        }
      }
    }

  }

  class DecoratorResolver(decorated: AnyRef) extends PropertyResolver {
    def hasPropertyValue(instance: Any, property: InjectableProperty) = property.isDelegate

    def getPropertyValue(instance: Any, property: InjectableProperty) =
      if (hasPropertyValue(instance, property))
        decorated
      else
        new NoSuchFieldException(property.getName)
  }

  class ReferenceWrapper[T <: AnyRef](val ref: T) extends Builder[T](ref.getClass) {
    def getModule = SyringeModule.this

    def makeClone(cloneName: String) = sys.error("Not supported")

    def addPropertyResolver(resolver: PropertyResolver) = null

    def decorateWith[D >: T](decorator: => Builder[D]) = null

    def delegateWith[D <: Interceptor[_]](provider: Delegation[D]) = null

    def build[D >: T] = ref

    def getPropertyValueConverter = null

    def setValueConverter(converter: PropertyValueConverter) = null

    def getInstanceName = null

    def syringeAllowMultiInjection = null
  }

  implicit def convertRefToBuilder[T <: AnyRef](ref: T): Builder[T] = new ReferenceWrapper[T](ref)

  implicit def convertAnyValToBuilder[T <: AnyVal](v: T): Builder[AnyRef] = new ReferenceWrapper[AnyRef](convertToRef(v))

  private def convertToRef(v: AnyVal): AnyRef = v match {
    case b: Byte => Byte.box(b)
    case s: Short => Short.box(s)
    case i: Int => Int.box(i)
    case l: Long => Long.box(l)
    case c: Char => Char.box(c)
    case f: Float => Float.box(f)
    case d: Double => Double.box(d)
    case b: Boolean => Boolean.box(b)
    case any => throw new AssertionError("Unexpected type: " + any.getClass.getName)
  }

  implicit def convertScalaListToJavaList[T](list: List[T]): java.util.List[T] = {
    val javaList = Lists.newArrayList[T]()
    list.reverse.foreach(e => javaList.add(e))
    javaList
  }

  private class ProxyDef(val factory: () => Builder[_ <: InvocationHandler],val interfaces: Array[Class[_]])
}
