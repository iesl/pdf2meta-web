import sbt._
import Keys._
import com.earldouglas.xsbtwebplugin.WebPlugin
import edu.umass.cs.iesl.sbtbase.IeslProject._
import edu.umass.cs.iesl.sbtbase.Dependencies

object Pdf2MetaWebBuild extends Build {

  val vers = "0.1-SNAPSHOT"

  implicit val allDeps = new Dependencies()
  import allDeps._
  val deps = Seq(
    ieslScalaCommons("latest.integration"),
    bibmogrify("latest.integration"),
    //pdf2meta("latest.integration"),
    liftWebkit(),
    liftMapper(),
    liftWizard(),
    scalatest(),
    classutil(),
    "javax.servlet" % "javax.servlet-api" % "3.0.1" % "provided",
    "org.eclipse.jetty" % "jetty-webapp" % "9.1.0.v20131115" % "container",
    "org.eclipse.jetty" % "jetty-plus"   % "9.1.0.v20131115" % "container",
    //jetty("6.1.26"),
    // jettyContainer("6.1.26"),
    "com.escalatesoft.subcut" %% "subcut" % "2.0",
    "com.typesafe" % "config" % "1.2.1",
    "net.liftmodules" %% "widgets_2.6" % "1.3"
  )

  lazy val pdf2meta = ProjectRef(new java.io.File("../pdf2meta"), "pdf2meta")

  lazy val pdf2metaWeb = Project("pdf2meta-web", new java.io.File("."))
    .ieslSetup(vers, deps, Public, WithSnapshotDependencies,"edu.umass.cs.iesl")
    .cleanLogging.standardLogging
    .settings(WebPlugin.webSettings :_*)
    .dependsOn(pdf2meta)

}

