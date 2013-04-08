# Syringe

Syringe is a dependency injection and configuration library from [AVAST Software](http://www.avast.com "AVAST Software") 
available for Java and Scala projects. More information can be found in the 
[wiki](https://github.com/avast-open/syringe/wiki "Syringe wiki").

## Code Example

### Components

```scala
    import com.avast.syringe.config.{ConfigProperty, ConfigBean}
    
    @ConfigBean
    class ComponentX {
    
      @ConfigProperty
      var compY: ComponentY = _
    
    }
    
    @ConfigBean
    class ComponentY {
    
      @ConfigProperty
      var message: String = _
    
    }
```

### Wiring

`MyModule` (aka __palette__) is generated via [Syringe Maven Plugin](https://github.com/avast-open/syringe-maven-plugin "Syringe Maven Plugin").
There can be many traits (aka __perspectives__) which can inherit from each other and override specific components.

```scala
    trait Perspective extends MyModule {
      
      lazy val msg = "Hello World!"
      
      override def newComponentX = super
                                     .newComponentX
                                     .compY(componentY)
      
      override def newComponentY = super
                                     .newComponentY
                                     .message(msg)
    
    }
```

### Run

```scala
    object MyApp extends Perspective {
    
      override def main(args: Array[String]) {
        // build the top-level component - recursively builds everything
        val compX = componentX.build
        println(compX.compY.message)
      }
      
    }
```

## Getting Started

To get started you need to setup your Maven POM as indicated below. Using `mvn clean` and `mvn package` you can 
regenerate your __palette__ (module) - there are two automatically enabled Maven profiles for that 
(`generate-palette` and `build-perspectives`).

```xml
    <dependencies>
        <dependency>
            <groupId>com.avast</groupId>
            <artifactId>syringe</artifactId>
            <version>1.3</version>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-clean-plugin</artifactId>
                <version>2.5</version>
                <configuration>
                    <filesets>
                        <fileset>
                            <directory>src/main/syringe</directory>
                            <includes>
                                <include>**/MyModule.scala</include>
                            </includes>
                            <followSymlinks>false</followSymlinks>
                        </fileset>
                    </filesets>
                </configuration>
            </plugin>
        </plugins>
    </build>

    <profiles>
        <profile>
            <id>generate-palette</id>
            <activation>
                <file>
                    <missing>src/main/syringe/com/avast/MyModule.scala</missing>
                </file>
            </activation>

            <build>
                <plugins>
                    <plugin>
                        <groupId>com.avast</groupId>
                        <artifactId>syringe-maven-plugin</artifactId>
                        <version>1.1</version>
                        <executions>
                            <execution>
                                <id>generate-module</id>
                                <goals>
                                    <goal>generate-module</goal>
                                </goals>
                                <configuration>
                                    <modulePackage>com.avast</modulePackage>
                                    <moduleName>MyModule</moduleName>
                                    <moduleDesc>Module description</moduleDesc>
                                    <generatedClassesDirectory>src/main/syringe</generatedClassesDirectory>
                                    <builderTraits>
                                        <property>
                                            <name>com\.avast\..*</name>
                                            <value>
                                                com.avast.syringe.config.perspective.JMXRegistry
                                            </value>
                                        </property>
                                    </builderTraits>
                                </configuration>
                            </execution>
                            <execution>
                                <phase>none</phase>
                            </execution>
                        </executions>
                    </plugin>
                </plugins>
            </build>
        </profile>

        <profile>
            <id>build-perspectives</id>
            <activation>
                <file>
                    <exists>src/main/syringe/com/avast/MyModule.scala</exists>
                </file>
            </activation>

            <build>
                <plugins>
                    <plugin>
                        <groupId>net.alchim31.maven</groupId>
                        <artifactId>scala-maven-plugin</artifactId>
                    </plugin>
                </plugins>
            </build>
        </profile>
    </profiles>
```
