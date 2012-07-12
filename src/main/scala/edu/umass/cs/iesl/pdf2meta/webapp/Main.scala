package edu.umass.cs.iesl.pdf2meta.webapp

import _root_.org.mortbay.jetty.Server
import _root_.org.mortbay.jetty.webapp.WebAppContext
import org.mortbay.jetty.nio._
import java.net.URL
import com.weiglewilczek.slf4s.Logging

// http://www.assembla.com/code/filesender2_proto/git/nodes/src/test/scala/RunWebApp.scala
object Main extends App with Logging {
  val port: Int = if (args.length > 0) args(0).toInt else 8080
  /*  val server = new Server
    val scc = new SelectChannelConnector

    scc.setPort(port)
    server.setConnectors(Array(scc))

    val context = new WebAppContext()
    context.setServer(server)
    context.setContextPath("/")
    //context.setWar("src/main/webapp")
    val appUrl: URL = getClass.getResource("/webapp")
    //if(appUrl == null) { logger.error("can't find webapp")}
    context.setWar(appUrl.toExternalForm())
  server.addHandler(context)
  */


  val server = new Server(port);
  val appUrl: URL = getClass.getResource("/webapp/")
  val appUrlString = appUrl.toExternalForm()
  server.setHandler(new WebAppContext(appUrlString, "/"));

  try {
    println(">>> STARTING EMBEDDED JETTY SERVER, PRESS ANY KEY TO STOP")
    server.start()
    while (System.in.available() == 0) {
      Thread.sleep(5000)
    }
    server.stop()
    server.join()
  } catch {
    case exc: Exception => {
      exc.printStackTrace()
      System.exit(100)
    }
  }
}
