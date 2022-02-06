import sbt._
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._
import org.portablescala.sbtplatformdeps.PlatformDepsPlugin.autoImport._


object Deps {

  val scalajsStub = Seq(("org.scala-js","scalajs-stubs","1.1.0"))
  val shared = Def.setting {
    (scalajsStub).map { case (org, lib, v) => org %% lib % v }
  }
}
