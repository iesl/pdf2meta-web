package edu.umass.cs.iesl.pdf2meta.webapp.lib
//import sys.process.{ProcessIO, Process}
import sys.process._
import java.io.File
import com.typesafe.scalalogging.slf4j.Logging
import scala.None
import edu.umass.cs.iesl.pdf2meta.webapp.cakesnippet.pageimages
import edu.umass.cs.iesl.scalacommons.Workspace
//import org.scala_tools.subcut.inject.AutoInjectable
import com.escalatesoft.subcut.inject.{Injectable, BindingModule}

import tools.nsc.io.File
/**
 * Created by klimzaporojets on 6/5/14.
 * Invokes psToText on a particular pdf file
 */
class PsToText(w: Workspace)(implicit val bindingModule: BindingModule) extends Logging with Injectable {

  //function to run any command not involving pipelining and redirections
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
    output + errors

  }

  println(inject[String]('examples))

  val filePath = inject[String]('pstotext_path)

  val outFilePsToText = filePath + "/" + w.filename + ".xml";

  val outFileRunCrf = filePath + "/" + w.filename + "_runcrf.xml";
  val outFilenameRunCrf = w.filename + "_runcrf.xml";
  val convertPath:String = inject[String]('pstotext)

  val runcrfFilePath = inject[String]('runcrf_path)

  //check if the pdf file exists already and if it is the same using diff command

  println ("result of executing diff " + filePath + "/" + w.filename + " " + w.file + ": " +
              runCommand("diff " + filePath + "/" + w.filename + " " + w.file ))

  println ("trim?: " + (runCommand("diff " + filePath + "/" + w.filename + " " + w.file).trim==""))

  println ("result of executing ls " + filePath + "/" + w.filename + ": " +
    runCommand("ls " + filePath + "/" + w.filename ))

  if(runCommand("ls " + filePath + "/" + w.filename ).contains("No such file") ||
                  runCommand("diff " + filePath + "/" + w.filename + " " + w.file).trim!="")
  {
    //if the files are not the same, the pdf is copied and all process is done
    runCommand("cp " + w.file + " " + filePath + "/" + w.filename)

    val output =
    {
      val result = (convertPath + " " + w.file).toString #> new java.io.File(outFilePsToText) !

      if(result!=0)
      {
        throw new PdfConversionException("Error while executing pstotext")
      }
      result
    }
    val f = new java.io.File(outFilePsToText)
    if(!f.exists)
    {
      throw new PdfConversionException("no xml file to parse found: " + outFilePsToText)
    }

    val resRuncrf = (sys.process.Process(Seq("echo", outFilePsToText + " -> " + outFileRunCrf)) #| sys.process.Process("bin/runcrf", new java.io.File(runcrfFilePath))).!!

  }


}

class PsToTextConversionException(s: String) extends Exception(s)
