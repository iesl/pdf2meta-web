package edu.umass.cs.iesl.pdf2meta.webapp.lib

import scala.sys.process.{Process, ProcessIO}
import com.typesafe.scalalogging.slf4j.Logging

/**
 * Created by klimzaporojets on 6/18/14.
 */
object linuxCommandExecuter extends Logging {
  def runCommand(commandToRun:String) =
  {

    logger.info("Running " + commandToRun)
    val pb = Process(commandToRun)
    val sb = StringBuilder.newBuilder
    val sbe = StringBuilder.newBuilder
    val pio = new ProcessIO(_ => (), stdout => scala.io.Source.fromInputStream(stdout).getLines().foreach(sb append _),
      stderr => scala.io.Source.fromInputStream(stderr).getLines().foreach(sbe append _))

    val p = pb.run(pio)
    val exitCode = p.exitValue()

    val output = sb toString()
    val errors = sbe toString()
    logger.info(output)
    logger.info(errors)
    logger.info("Finished running (" + exitCode + ") " + commandToRun)
    output
  }
}
