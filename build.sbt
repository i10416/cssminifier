import sbt._
import sbtcrossproject.CrossPlugin.autoImport.crossProject

val scala213 = "2.13.8"

val scala3 = "3.2.0"
lazy val projectName = "cssminifier"
val githubId = "i10416"
resolvers += Resolver.sonatypeRepo("snapshots")
// WARN: Make sure build.sbt does not define any of the following settings(https://github.com/sbt/sbt-ci-release)

// todo: add task to create github repository from this
inThisBuild(
  Seq(
    versionScheme := Some("early-semver"),
    organization := "dev.i10416",
    licenses := List(
      "Apache-2.0" -> url("https://www.apache.org/licenses/LICENSE-2.0")
    ),
    homepage := Some(url(s"https://github.com/$githubId/$projectName")),
    developers := List(
      Developer(
        githubId,
        "i10416",
        s"ito.yo16uh90616+$projectName@gmail.com",
        url(s"https://github.com/$githubId")
      )
    ),
    sonatypeCredentialHost := "s01.oss.sonatype.org",
    sonatypeRepository := "https://s01.oss.sonatype.org/service/local",
    scalacOptions ++= Seq("-feature", "-deprecation")
  )
)

lazy val root = project
  .in(file("."))
  .aggregate(lib.js, lib.jvm, lib.native)
  .settings(
    publish / skip := true
  )

lazy val lib = crossProject(JSPlatform, JVMPlatform, NativePlatform)
  .in(file("."))
  .settings(
    libraryDependencies ++= Deps.shared.value,
    name := projectName,
    scalaVersion := scala213
  )
  .jsSettings(
    crossScalaVersions := Seq(scala213, scala3),
    libraryDependencies += ("org.scalameta" %%% "munit" % "1.0.0-M6") % Test
  )
  .jvmSettings(
    crossScalaVersions := Seq(scala213, scala3),
    libraryDependencies += ("org.scalameta" %%% "munit" % "1.0.0-M6") % Test
  )
  .nativeSettings(
    crossScalaVersions := Seq(scala213, scala3),
    libraryDependencies ++=
      Seq(
        "org.scalameta" %%% "munit" % "1.0.0-M6" % Test
      )
  )
