import sbt.Package.ManifestAttributes

organization := "com.avast"
name := "syringe"
scalaVersion := "2.10.5"
crossScalaVersions := Seq("2.10.5", "2.11.7")
javacOptions ++= Seq("-source", "1.8")
javacOptions ++= Seq("-target", "1.8")
packageOptions := Seq(
  ManifestAttributes(
    ("Build-Timestamp", System.currentTimeMillis().toString)
  )
)

libraryDependencies ++= Seq(
  "com.google.code.findbugs" % "jsr305" % "2.0.1",
  "org.slf4j" % "slf4j-api" % "1.7.2",
  "org.freemarker" % "freemarker" % "2.3.19",
  "com.google.guava" % "guava" % "14.0",
  "xom" % "xom" % "1.2.5",
  "cglib" % "cglib" % "2.2.2",
  "commons-io" % "commons-io" % "2.1",
  "javax.inject" % "javax.inject" % "1",
  "junit" % "junit" % "4.10" % "test",
  "org.scalatest" %% "scalatest" % "2.2.2" % "test"
)

import ReleaseKeys._
releaseSettings
crossBuild := true
publishArtifactsAction := PgpKeys.publishSigned.value

pomExtra := {
  <url>https://github.com/avast/syringe</url>
  <licenses>
    <license>
      <name>The BSD 3-Clause License</name>
      <url>http://opensource.org/licenses/BSD-3-Clause</url>
    </license>
  </licenses>
  <developers>
    <developer>
      <id>slajchrt</id>
      <name>Zbynek Slajchrt (original creator)</name>
      <email>slajchrt@avast.com</email>
    </developer>
    <developer>
      <id>jakubjanecek</id>
      <name>Jakub Janecek</name>
      <email>janecek@avast.com</email>
      <organization>AVAST Software</organization>
      <organizationUrl>http://www.avast.com</organizationUrl>
    </developer>
  </developers>
  <scm>
    <connection>scm:git:git@github.com:avast/syringe.git</connection>
    <developerConnection>scm:git:git@github.com:avast/syringe.git</developerConnection>
    <url>git@github.com:avast/syringe.git</url>
  </scm>
}
