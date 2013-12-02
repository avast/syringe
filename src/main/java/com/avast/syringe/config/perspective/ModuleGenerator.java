package com.avast.syringe.config.perspective;

import com.avast.syringe.*;
import com.avast.syringe.config.ConfigBean;
import com.avast.syringe.config.internal.ConfigClassAnalyzer;
import com.avast.syringe.config.internal.InjectableProperty;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;

import java.io.Writer;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <pre>
 *  {
 *      package
 *      moduleDesc
 *      moduleName
 *      moduleTraits
 *      classDescriptors: [{
 *          name
 *          simpleName
 *          builderMethodName
 *          builderTraits
 *          propertyDescriptors: [{
 *              name
 *              type
 *          }]
 *      }]
 * </pre>
 * <p/>
 * User: slajchrt
 * Date: 6/4/12
 * Time: 5:28 PM
 */
public class ModuleGenerator {

    public class ModuleDescriptor {

        public class ClassDescriptor {

            public class PropertyDescriptor {

                public class Argument {
                    private String name;
                    private String type;

                    public Argument(String name, String type) {
                        this.name = name;
                        this.type = type;
                    }

                    public String getName() {
                        return name;
                    }

                    public String getType() {
                        return type;
                    }
                }

                final String name;
                final String setterName;
                final List<Argument> arguments;

                public PropertyDescriptor(InjectableProperty prop) {
                    if (prop.isCollection() || prop.isArray()) {
                        setterName = "addTo" + makeFirstUpperCase(prop.getName());
                        arguments = Collections.singletonList(
                                new Argument("value", convertToScalaType(prop.getArrayOrCollectionComponentType())));
                    } else if (prop.isMap()) {
                        setterName = "putTo" + makeFirstUpperCase(prop.getName());
                        arguments = Arrays.asList(
                                new Argument("key", convertToScalaType(prop.getMapKeyType())),
                                new Argument("value", convertToScalaType(prop.getMapValueType()))
                        );
                    } else {
                        setterName = prop.getName();
                        arguments = Collections.singletonList(new Argument("value", convertToScalaType(prop.getType())));
                    }
                    this.name = prop.getName();
                }

                public String getName() {
                    return name;
                }

                public String getSetterName() {
                    return setterName;
                }

                public List<Argument> getArguments() {
                    return arguments;
                }
            }

            final String name;
            final String simpleName;
            final String builderMethodName;
            final String singletonBuilderMethodName;
            final List<PropertyDescriptor> propertyDescriptors;
            final List<String> genericParameters = Lists.newArrayList();
            final boolean generic;
            final Set<String> builderTraits = Sets.newLinkedHashSet();

            private final boolean provider;

            final String typeName;
            final List<String> typeGenericParameters = Lists.newArrayList();
            final boolean typeGeneric;

            public ClassDescriptor(Class<?> cls, Properties builderTraitMappings) {
                provider = com.avast.syringe.Provider.class.isAssignableFrom(cls);
                Type typeType = provider ? findProviderParameter(cls) : cls;
                Class typeCls = getClassForType(typeType);

                if (provider) {
                    name = cls.getName();
                    typeName = typeCls.getName();
                } else {
                    name = typeName = cls.getName();
                }

                ConfigBean cfgBeanAnnot = cls.getAnnotation(ConfigBean.class);
                if (cfgBeanAnnot != null && !"".equals(cfgBeanAnnot.value())) {
                    simpleName = cfgBeanAnnot.value();
                } else {
                    simpleName = cls.getSimpleName();
                }

                builderMethodName = "new" + simpleName;
                singletonBuilderMethodName = makeFirstLowerCase(simpleName);

                propertyDescriptors = Lists.transform(new ConfigClassAnalyzer(cls).getConfigProperties(),
                        new Function<InjectableProperty, PropertyDescriptor>() {
                            @Override
                            public PropertyDescriptor apply(InjectableProperty prop) {
                                return new PropertyDescriptor(prop);
                            }
                        });

                TypeVariable[] clsParameters = cls.getTypeParameters();
                generic = clsParameters.length > 0;

                for (TypeVariable clsParameter : clsParameters) {
                    genericParameters.add(constructGenericScalaPlaceholder(clsParameter));
                }

                if (provider) {
                    TypeVariable[] typeParameters = typeCls.getTypeParameters();
                    typeGeneric = typeParameters.length > 0;

                    for (TypeVariable typeParameter : typeParameters) {
                        typeGenericParameters.add(constructGenericScalaPlaceholder(typeParameter));
                    }

                } else {
                    typeGeneric = generic;
                    typeGenericParameters.addAll(genericParameters);
                }

                if (builderTraitMappings != null) {
                    for (Map.Entry<Object, Object> builderTraitMapping : builderTraitMappings.entrySet()) {
                        String productClassNamePatternSource = builderTraitMapping.getKey().toString();
                        Pattern productClassNamePattern = Pattern.compile(productClassNamePatternSource);
                        Matcher matcher = productClassNamePattern.matcher(cls.getName());
                        if (matcher.matches()) {
                            String builderTraitsSource = builderTraitMapping.getValue().toString();
                            Iterable<String> splitTraits = Splitter.on("\n").trimResults().split(builderTraitsSource);
                            for (String splitTrait : splitTraits) {
                                builderTraits.add(splitTrait);
                            }

                        }
                    }
                }
            }

            public String getName() {
                return name;
            }

            public String getSimpleName() {
                return simpleName;
            }

            public String getBuilderMethodName() {
                return builderMethodName;
            }

            public String getSingletonBuilderMethodName() {
                return singletonBuilderMethodName;
            }

            public List<PropertyDescriptor> getPropertyDescriptors() {
                return propertyDescriptors;
            }

            public List<String> getGenericParameters() {
                return genericParameters;
            }

            public boolean isProvider() {
                return provider;
            }

            public boolean isGeneric() {
                return generic;
            }

            public Set<String> getBuilderTraits() {
                return builderTraits;
            }

            public String getTypeName() {
                return typeName;
            }

            public List<String> getTypeGenericParameters() {
                return typeGenericParameters;
            }

            public boolean isTypeGeneric() {
                return typeGeneric;
            }
        }

        final String pkg;
        final String moduleDesc;
        final String moduleName;
        final List<ClassDescriptor> classDescriptors;
        final List<String> moduleTraits;

        public ModuleDescriptor(String pkg, String moduleDesc, String moduleName, List<Class> classes,
                                List<String> moduleTraits, final Properties builderTraitMappings) {
            this.pkg = pkg;
            this.moduleDesc = moduleDesc;
            this.moduleName = moduleName;
            this.moduleTraits = moduleTraits == null ? Collections.<String>emptyList() : moduleTraits;

            ImmutableList<Class> nonAbstractClasses = ImmutableList.copyOf(Iterables.filter(classes, new Predicate<Class>() {
                @Override
                public boolean apply(Class cls) {
                    return !Modifier.isAbstract(cls.getModifiers());
                }
            }));

            this.classDescriptors = Lists.transform(nonAbstractClasses, new Function<Class, ClassDescriptor>() {
                @Override
                public ClassDescriptor apply(Class cls) {
                    return new ClassDescriptor(cls, builderTraitMappings);
                }
            });
        }

        public String getPkg() {
            return pkg;
        }

        public String getModuleDesc() {
            return moduleDesc;
        }

        public String getModuleName() {
            return moduleName;
        }

        public List<ClassDescriptor> getClassDescriptors() {
            return classDescriptors;
        }

        public List<String> getModuleTraits() {
            return moduleTraits;
        }
    }

    private static final ModuleGenerator INSTANCE = new ModuleGenerator();

    private final Configuration fmCfg;

    private ModuleGenerator() {
        fmCfg = new Configuration();
        fmCfg.setClassForTemplateLoading(ModuleGenerator.class, "");
        fmCfg.setObjectWrapper(new DefaultObjectWrapper());
        fmCfg.setTemplateLoader(new OsSpecificTemplateLoader(fmCfg.getTemplateLoader()));
    }

    public static ModuleGenerator getInstance() {
        return INSTANCE;
    }

    public void generate(String pkg, String moduleDesc, String module, List<Class> classes, List<String> moduleTraits,
                         Properties builderTraitMappings, Writer writer)
            throws Exception {
        Template template = fmCfg.getTemplate("module-template.ftl");
        ModuleDescriptor model = new ModuleDescriptor(pkg, moduleDesc, module, classes, moduleTraits,
                builderTraitMappings);
        template.process(model, writer);
        writer.flush();
    }

    private static String convertToScalaType(Class type) {
        final String typeName = type.getName();

        if ("byte".equals(typeName) || Byte.class.getName().equals(typeName)) {
            return "Byte";
        }
        if ("short".equals(typeName) || Short.class.getName().equals(typeName)) {
            return "Short";
        }
        if ("int".equals(typeName) || Integer.class.getName().equals(typeName)) {
            return "Int";
        }
        if ("long".equals(typeName) || Long.class.getName().equals(typeName)) {
            return "Long";
        }
        if ("float".equals(typeName) || Float.class.getName().equals(typeName)) {
            return "Float";
        }
        if ("double".equals(typeName) || Double.class.getName().equals(typeName)) {
            return "Double";
        }
        if ("char".equals(typeName) || Character.class.getName().equals(typeName)) {
            return "Char";
        }
        if ("boolean".equals(typeName) || Boolean.class.getName().equals(typeName)) {
            return "Boolean";
        }
        if (String.class.getName().equals(typeName)) {
            return "String";
        }

        //return SyringeModule.SyringeBuilder.class.getSimpleName() + "[" +
        return Builder.class.getSimpleName() + "[" +
                constructGenericScalaTypeName(type) + "]";
    }

    private static String constructGenericScalaTypeName(Class type) {
        // TODO:
        return "_";
    }

    private static String constructGenericScalaPlaceholder(TypeVariable typeParameter) {
        // TODO:
        return "_";
    }

    private static String makeFirstUpperCase(String name) {
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    private static String makeFirstLowerCase(String name) {
        return Character.toLowerCase(name.charAt(0)) + name.substring(1);
    }

    static Class getClassForType(Type t) {
        if (t instanceof Class) {
            return (Class) t;
        } if (t instanceof ParameterizedType) {
            Type rawType = ((ParameterizedType) t).getRawType();
            if (rawType instanceof Class) {
                return (Class) rawType;
            } else {
                throw new IllegalArgumentException("Unsupported type " + t);
            }
        } else {
            throw new IllegalArgumentException("Unsupported type " + t);
        }
    }

    static Type findProviderParameter(Class<?> cls) {
        Type[] genericInterfaces = cls.getGenericInterfaces();
        for (Type genericInterface : genericInterfaces) {
            if (genericInterface instanceof ParameterizedType &&
                    ((ParameterizedType) genericInterface).getRawType() instanceof Class &&
                    com.avast.syringe.Provider.class.isAssignableFrom((Class)((ParameterizedType) genericInterface).getRawType())) {

                if (((ParameterizedType) genericInterface).getRawType() == com.avast.syringe.Provider.class) {
                    Type providerParamType = ((ParameterizedType) genericInterface).getActualTypeArguments()[0];
                    if (providerParamType instanceof Class) {
                        return providerParamType;
                    } if (providerParamType instanceof ParameterizedType) {
                        return providerParamType;
                    } else {
                        throw new IllegalArgumentException("Provider class " + cls + " must specify the product type");
                    }
                } else {
                    return findProviderParameter((Class)((ParameterizedType) genericInterface).getRawType());
                }

            }
        }

        if (cls == Object.class || cls.getSuperclass() == Object.class) {
            throw new IllegalArgumentException("Provider class " + cls + " must specify the product type");
        }

        try {
            return findProviderParameter(cls.getSuperclass());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Provider class " + cls + " must specify the product type\n", e);
        }
    }


}
