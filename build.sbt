sourcesInBase := false


lazy val commonSettings = Seq(
  organization := "com.github.marcinzh",
  version := "0.5.0-SNAPSHOT",
  scalaVersion := "2.12.2",
  crossScalaVersions := Seq(scalaVersion.value, "2.11.11"),
  scalacOptions ++= Seq(
    "-language:implicitConversions",
    "-language:higherKinds",
    "-unchecked", 
    "-feature", 
    "-deprecation"
    // "-Xlint:_",
    // "-Xlint:-infer-any",
    // "-Xfatal-warnings",
    // "-Ywarn-unused:-params,-implicits"
    // "-Ywarn-unused:imports,privates,-patvars,-locals,-params,-implicits"
  ),
  resolvers += Resolver.sonatypeRepo("releases"),
  libraryDependencies += compilerPlugin("org.spire-math" %% "kind-projector" % "0.9.3"),

)


lazy val dontPublishMe = Seq(
  publishTo := None,
  publish := (),
  publishLocal := (),
  publishArtifact := false
)


lazy val root = project
  .in(file("."))
  .settings(name := "skutek-root")
  .settings(commonSettings: _*)
  .settings(dontPublishMe: _*)
  .aggregate(core, examples)


lazy val core = project
  .in(file("core"))
  .settings(name := "skutek-core")
  .settings(commonSettings: _*)
  .settings(libraryDependencies += "org.specs2" %% "specs2-core" % "3.9.1" % "test")
  .settings(libraryDependencies += "org.specs2" %% "specs2-matcher-extra" % "3.9.1" % "test")
  .settings(scalacOptions in Test += "-Yrangepos")

lazy val examples = project
  .in(file("examples"))
  .settings(name := "skutek-examples")
  .settings(commonSettings: _*)
  .settings(dontPublishMe: _*)
  .dependsOn(core)
  
