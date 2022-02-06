import sbt._
import sbtcrossproject.CrossPlugin.autoImport.crossProject

val scala213 = "2.13.8"
val scala3 = "3.1.0"
lazy val projectName = "cssminifier"
val githubId = "i10416"
resolvers += Resolver.sonatypeRepo("snapshots")
// WARN: Make sure build.sbt does not define any of the following settings(https://github.com/sbt/sbt-ci-release)
lazy val publishSettings = Seq(
  sonatypeCredentialHost := "s01.oss.sonatype.org",
  sonatypeRepository := "https://s01.oss.sonatype.org/service/local"
)

// todo: add task to create github repository from this
inThisBuild(
  Seq(
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
    scalacOptions ++= Nil
  )
)

lazy val lib = crossProject(JSPlatform, JVMPlatform, NativePlatform)
  .in(file("."))
  .settings(
    libraryDependencies ++= Deps.shared.value,
    // munit has not released the version which supports scala native in scala 3
    // libraryDependencies += "org.scalameta" %%% "munit" % "1.0.0-M1" % Test,
    name := projectName,
    scalaVersion := scala3,
    crossScalaVersions := Seq(scala213, scala3),
    // workaround for compile error(3.1.0)
    // will be fixed in scala 3.1.1
    Compile / doc / scalacOptions --= scalaVersionsDependendent(
      scalaVersion.value
    )(Seq.empty[String]) {
      case (2, 11) => Seq("-Xfatal-warnings")
      case (3, _)  =>
        // Remove all plugins as they lead to exceptions
        (Compile / doc / scalacOptions).value
          .filter(_.contains("-Xplugin"))
    }
  )
  .jsSettings()
  .jvmSettings()
// taken from https://github.com/scala-native/scala-native/blob/c0403fbb608e696f967ea12a6384d3d301d8f2ae/project/Settings.scala#L706
def scalaVersionsDependendent[T](scalaVersion: String)(default: T)(
    matching: PartialFunction[(Long, Long), T]
): T =
  CrossVersion
    .partialVersion(scalaVersion)
    .collect(matching)
    .getOrElse(default)
