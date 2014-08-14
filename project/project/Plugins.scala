import sbt._
import sbt.Keys._
//import com.earldouglas.xsbtwebplugin.WebPlugin


object IeslPluginLoader extends Build {

  lazy val root = Project(id = "plugins", base = file("."))
    .settings(resolvers += "IESL Public Releases" at "https://dev-iesl.cs.umass.edu/nexus/content/groups/public")
    .settings(resolvers += "IESL Public Snapshots" at "https://dev-iesl.cs.umass.edu/nexus/content/groups/public-snapshots")
    .settings(resolvers += "IESL Public Snapshots" at "http://scalasbt.artifactoryonline.com/scalasbt/sbt-plugin-releases/")
    .settings(addSbtPlugin("edu.umass.cs.iesl" %% "iesl-sbt-base" % "80"))  // optional: latest.snapshot
    .settings(addSbtPlugin("com.earldouglas" % "xsbt-web-plugin" % "0.9.0"))
  //    .settings(addSbtPlugin("com.typesafe.sbt" % "sbt-start-script" % "0.6.0"))

}
