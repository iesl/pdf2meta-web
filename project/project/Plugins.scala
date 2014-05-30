import sbt._
import sbt.Keys._
//import com.earldouglas.xsbtwebplugin.WebPlugin


object IeslPluginLoader extends Build {

  //Project(..., settings = Project.defaultSettings ++ WebPlugin.webSettings)

  lazy val root = Project(id = "plugins", base = file("."))
    .settings(resolvers += "IESL Public Releases" at "https://dev-iesl.cs.umass.edu/nexus/content/groups/public")
    .settings(resolvers += "IESL Public Snapshots" at "https://dev-iesl.cs.umass.edu/nexus/content/groups/public-snapshots")
    .settings(resolvers += "IESL Public Snapshots" at "http://scalasbt.artifactoryonline.com/scalasbt/sbt-plugin-releases/")
    //kzaporojets adding resolver for lift plugin
//    .settings(resolvers += "sbt-plugin-releases" at "https://dev-iesl.cs.umass.edu/nexus/content/groups/public-snapshots")

    .settings(addSbtPlugin("edu.umass.cs.iesl" % "iesl-sbt-base" % "latest.release"))  // optional: latest.snapshot
    .settings(addSbtPlugin("com.earldouglas" % "xsbt-web-plugin" % "0.10.0-SNAPSHOT"))
//    .settings(addSbtPlugin("com.typesafe.sbt" % "sbt-start-script" % "0.6.0"))

  //kzaporojets: adding lifty
//    .settings(addSbtPlugin("org.lifty" % "lifty" % "1.7.4"))
}
//https://github.com/earldouglas/xsbt-web-plugin
// as of sbt 0.12.0 we can rebuild the plugin on the fly from the hg repository,
// avoiding the Nexus URL chicken-and-egg problem (or rather, pushing it back one level to the Bitbucket URL)

/*
import sbt._

object IeslPluginLoader extends Build {
  override lazy val projects = Seq(root)
  lazy val root = Project("plugins", file(".")) dependsOn( ieslSbtBase )
  lazy val ieslSbtBase = uri("hg:ssh://bitbucket.org/IESL/iesl-sbt-base")
}
 */
