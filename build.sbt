sourcesInBase := false

lazy val commonSettings = Seq(
  organization := "com.github.marcinzh",
  version := "0.10.0-SNAPSHOT",
  scalaVersion := "2.12.8",
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
  libraryDependencies += compilerPlugin("org.spire-math" %% "kind-projector" % "0.9.8"),
  licenses := Seq("MIT" -> url("http://opensource.org/licenses/MIT")),
  resolvers += Resolver.jcenterRepo,
  credentials += Credentials(Path.userHome / ".bintray" / ".credentials"),
  publishTo := Some("Bintray API Realm" at ("https://api.bintray.com/content/marcinzh/maven/skutek/" ++ version.value))
)

lazy val lessCommonSettings = Seq(
  libraryDependencies += compilerPlugin("com.olegpy" %% "better-monadic-for" % "0.2.3"),
)

lazy val testSettings = Seq(
  libraryDependencies += "org.specs2" %% "specs2-core" % "3.9.1" % "test",
  libraryDependencies += "org.specs2" %% "specs2-matcher-extra" % "3.9.1" % "test",
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
  .aggregate(core, examples, experimental)


lazy val core = project
  .in(file("core"))
  .settings(name := "skutek-core")
  .settings(commonSettings: _*)
  .settings(testSettings: _*)

lazy val experimental = project
  .in(file("experimental"))
  .settings(name := "skutek-experimental")
  .settings(commonSettings: _*)
  .settings(lessCommonSettings: _*)
  .settings(testSettings: _*)
  .dependsOn(core)

lazy val examples = project
  .in(file("examples"))
  .settings(name := "skutek-examples")
  .settings(commonSettings: _*)
  .settings(lessCommonSettings: _*)
  .settings(dontPublishMe: _*)
  .dependsOn(core)
  
