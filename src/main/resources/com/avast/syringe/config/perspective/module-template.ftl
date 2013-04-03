package ${pkg}
import com.avast.syringe.config.perspective._
import javax.annotation.Generated
/**
 * ${moduleDesc}
 *
 */
@Generated(value = Array("Syringe"))
trait ${moduleName} extends SyringeModule
<#list moduleTraits as moduleTrait>with ${moduleTrait}
</#list>
{

<#list classDescriptors as cls>
object ${cls.simpleName}Builder
{
    type instanceType = <#if cls.provider>com.avast.syringe.Provider[</#if>${cls.typeName}<#if cls.typeGeneric>[<#list cls.typeGenericParameters as genPar>${genPar}<#if genPar_has_next>, </#if></#list>]</#if><#if cls.provider>]</#if>
    val instanceClass = classOf[${cls.name}<#if cls.generic>[<#list cls.genericParameters as genPar>${genPar}<#if genPar_has_next>, </#if></#list>]</#if>]
}
class ${cls.simpleName}Builder (instanceClass: Class[_]) extends SyringeBuilder[${cls.simpleName}Builder.instanceType](instanceClass)
<#list cls.builderTraits as builderTrait>with ${builderTrait}[${cls.simpleName}Builder.instanceType]
</#list>
{
    def this() = this(${cls.simpleName}Builder.instanceClass)
    def set(propertyName: String, value: => Any): this.type = inject(propertyName, value)
    <#list cls.propertyDescriptors as prop>
    def ${prop.setterName}(<#list prop.arguments as arg>${arg.name}: => ${arg.type}<#if arg_has_next>, </#if></#list>): this.type = inject("${prop.name}", <#list prop.arguments as arg>${arg.name}<#if arg_has_next>, </#if></#list>)
    </#list>
}

def ${cls.builderMethodName} = new ${cls.simpleName}Builder().initialize
lazy val ${cls.singletonBuilderMethodName} = __sm__[${cls.simpleName}Builder](${cls.builderMethodName})

</#list>
}
