sourcesInBase := false

lazy val commonSettings = Seq(
  organization := "com.github.marcinzh",
  version := "0.12.0-SNAPSHOT",
  scalaVersion := "2.12.10",
  crossScalaVersions := Seq(scalaVersion.value, "2.11.12"),
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
  libraryDependencies += compilerPlugin("org.typelevel" % "kind-projector" % "0.10.0" cross CrossVersion.binary),
  licenses := Seq("MIT" -> url("http://opensource.org/licenses/MIT")),
  resolvers += Resolver.jcenterRepo,
  credentials += Credentials(Path.userHome / ".bintray" / ".credentials"),
  publishTo := Some("Bintray API Realm" at ("https://api.bintray.com/content/marcinzh/maven/skutek/" ++ version.value))
)

lazy val commonExceptCoreSettings = Seq(
  libraryDependencies += compilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.0"),
)

lazy val testSettings = Seq(
  libraryDependencies += "org.specs2" %% "specs2-core" % "4.3.4" % "test",
  libraryDependencies += "org.specs2" %% "specs2-matcher-extra" % "4.3.4" % "test",
  parallelExecution in Test := false,
  scalacOptions in Test += "-Yrangepos",
)


lazy val dontPublishMe = Seq(
  publishTo := None,
  publish := (()),
  publishLocal := (()),
  publishArtifact := false
)


lazy val root = project
  .in(file("."))
  .settings(name := "skutek-root")
  .settings(commonSettings: _*)
  .settings(dontPublishMe: _*)
  .aggregate(core, examples, experimental, mwords)

lazy val mwords = project
  .in(file("modules/mwords"))
  .settings(name := "skutek-mwords")
  .settings(commonSettings: _*)
  .settings(commonExceptCoreSettings: _*)

lazy val core = project
  .in(file("modules/core"))
  .settings(name := "skutek-core")
  .settings(commonSettings: _*)
  .settings(testSettings: _*)
  .dependsOn(mwords)

lazy val experimental = project
  .in(file("modules/experimental"))
  .settings(name := "skutek-experimental")
  .settings(commonSettings: _*)
  .settings(commonExceptCoreSettings: _*)
  .settings(testSettings: _*)
  .dependsOn(core)

lazy val examples = project
  .in(file("modules/examples"))
  .settings(name := "skutek-examples")
  .settings(commonSettings: _*)
  .settings(commonExceptCoreSettings: _*)
  .settings(dontPublishMe: _*)
  .dependsOn(core)
  
