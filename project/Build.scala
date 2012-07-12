import com.github.siasia.WebPlugin
import sbt._
import edu.umass.cs.iesl.sbtbase.IeslProject
import edu.umass.cs.iesl.sbtbase.IeslProject._
import edu.umass.cs.iesl.sbtbase.Dependencies._

// this is just an example, to show how simple a build can be once all the boilerplate stuff is factored out.

object Pdf2MetaWebBuild extends Build {

  val vers = "0.1-SNAPSHOT"

  val deps = Seq(

    ieslScalaCommons("latest.integration"),
    bibmogrify("latest.integration"),
    pdf2meta("latest.integration"),
    liftWebkit(),
    liftMapper(),
    liftWizard(),
    scalatest(),
    classutil(),
    jetty("6.1.26"),
    jettyContainer("6.1.26")
  )


  lazy val pdf2metaWeb = IeslProject("pdf2meta-web ", vers, deps, Public,WithSnapshotDependencies)
    .settings(addCompilerPlugin("org.scala-tools.subcut" %% "subcut" % "1.0"))
    .settings(WebPlugin.webSettings :_*)
 /*   .settings((resourceGenerators in Compile <+= (resourceManaged, baseDirectory) map
    { (managedBase, base) =>
      val webappBase = base / "src" / "main" / "webapp"
      for {
        (from, to) <- webappBase ** "*" x rebase(webappBase, managedBase /
          "main" / "webapp")
      } yield {
        Sync.copy(from, to)
        to
      }
    }))*/

}

/*
//import AssemblyKeys._ // put this at the top of the file

name := "pdf2meta-web"

organization := "edu.umass.cs.iesl"

version := "0.1-SNAPSHOT"

scalaVersion := "2.9.1"

libraryDependencies +=  "edu.umass.cs.iesl" %% "scalacommons" % "0.1-SNAPSHOT"  changing()

libraryDependencies +=  "edu.umass.cs.iesl" %% "bibmogrify" % "0.1-SNAPSHOT"  changing()

libraryDependencies +=  "edu.umass.cs.iesl" %% "pdf2meta" % "0.1-SNAPSHOT"  changing()

libraryDependencies += "net.liftweb" %% "lift-webkit" % "2.4-M5" % "compile->default"

libraryDependencies += "net.liftweb" %% "lift-mapper" % "2.4-M5" % "compile->default"

libraryDependencies += "net.liftweb" %% "lift-wizard" % "2.4-M5" % "compile->default"

libraryDependencies += "org.scalatest" %% "scalatest" % "1.6.1" % "test"

libraryDependencies += "org.clapper" %% "classutil" % "0.4.3"

resolvers += "IESL Repo" at "https://dev-iesl.cs.umass.edu/nexus/content/repositories/releases"

resolvers += "IESL Snapshot Repo" at "https://dev-iesl.cs.umass.edu/nexus/content/repositories/snapshots"

resolvers += "David Soergel Repo" at "http://dev.davidsoergel.com/nexus/content/groups/public"

resolvers += "David Soergel Snapshots" at "http://dev.davidsoergel.com/nexus/content/repositories/snapshots"

//seq(assemblySettings: _*)

//seq(webSettings :_*)

addCompilerPlugin("org.scala-tools.subcut" %% "subcut" % "1.0")

resourceGenerators in Compile <+= (resourceManaged, baseDirectory) map
{ (managedBase, base) =>
  val webappBase = base / "src" / "main" / "webapp"
  for {
    (from, to) <- webappBase ** "*" x rebase(webappBase, managedBase /
"main" / "webapp")
  } yield {
    Sync.copy(from, to)
    to
  }
}

libraryDependencies += "org.mortbay.jetty" % "jetty" % "6.1.22" % "container"

libraryDependencies += "org.mortbay.jetty" % "jetty" % "6.1.22"

publishTo <<= (version)
                                            {version: String =>
                                              {
                                              def repo(name: String) = name at "https://dev-iesl.cs.umass.edu/nexus/content/repositories/" + name
                                              val isSnapshot = version.trim.endsWith("SNAPSHOT")
                                              val repoName = if (isSnapshot) "snapshots" else "releases"
                                              Some(repo(repoName))
                                              }
                                            }

credentials +=
                                  {
                                  Seq("build.publish.user", "build.publish.password").map(k => Option(System.getProperty(k))) match
                                  {
                                    case Seq(Some(user), Some(pass)) =>
                                      Credentials("Sonatype Nexus Repository Manager", "dev-iesl.cs.umass.edu", user, pass)
                                    case _ =>
                                      Credentials(Path.userHome / ".ivy2" / ".credentials")
                                  }
                                  }

*/
