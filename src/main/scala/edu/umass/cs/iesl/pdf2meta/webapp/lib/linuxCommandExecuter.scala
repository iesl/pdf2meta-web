package edu.umass.cs.iesl.pdf2meta.webapp.lib

import scala.sys.process.{Process, ProcessIO}
import com.typesafe.scalalogging.slf4j.Logging

/**
 * Created by klimzaporojets on 6/18/14.
 */
object linuxCommandExecuter extends Logging {
  def runCommand(commandToRun:List[String]) =
  {

    logger.info("Running " + commandToRun)
    //val tempCmd = List("cp","/var/folders/8w/0mfs0k2x0fqdj0bxk7843m140000gn/T/UgdMAO349412179499175432.tmp/8_OM-et al-focusedtaxonomies.pdf", "/pstotext/data/8_OM-et al-focusedtaxonomies/");
    val pb = Process(commandToRun /*commandToRun*/)
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
