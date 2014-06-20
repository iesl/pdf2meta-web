package edu.umass.cs.iesl.pdf2meta.webapp.lib
//import sys.process.{ProcessIO, Process}
import sys.process._
//import java.io.File
import com.typesafe.scalalogging.slf4j.Logging
import scala.None
import edu.umass.cs.iesl.pdf2meta.webapp.cakesnippet.{pageimages, pagexmls}
import edu.umass.cs.iesl.scalacommons.Workspace
import net.liftweb.http.S
import scala.reflect.io.Directory
import edu.umass.cs.iesl.pdf2meta.webapp.lib.MetataggerXmlLogic.PageXml

//import org.scala_tools.subcut.inject.AutoInjectable
import com.escalatesoft.subcut.inject.{Injectable, BindingModule}

//import tools.nsc.io.File

import scala.tools.nsc.io.File
/**
 * Created by klimzaporojets on 6/5/14.
 * Invokes psToText on a particular pdf file
 */
class PsToText(w: Workspace)(implicit val bindingModule: BindingModule) extends Logging with Injectable {

  val props:MapToProperties = new MapToProperties()
  val propertiesFilename:String =
    S.get("propertiesFile").openOrThrowException("err while obtaining properties file")

  val properties:Map[String,String] = props.readPropertiesFile(propertiesFilename)
  val outFilenameRunCrf = w.filename + "_runcrf.xml";
  val filePath = w.dir.path //inject[String]('pstotext_path)
  val outFileRunCrf = filePath + "/" + w.filename + "_runcrf.xml";

  if(properties.get("ispdfalreadyparsed").get=="false")
  {
    S.set("state","uploading")
    S.set("message","20%: running pstotext")
    S.set("percentage","20")

    val outFilePsToText = filePath + "/" + w.filename + ".xml";

    val convertPath:String = inject[String]('pstotext)

    val runcrfFilePath = inject[String]('runcrf_path)

    //check if the pdf file exists already and if it is the same using diff command

      val output = {
        val result = (convertPath + " " + w.file).toString #> new java.io.File(outFilePsToText) !

        if (result != 0) {
          throw new PdfConversionException("Error while executing pstotext")
        }
        result
      }
      val f = new java.io.File(outFilePsToText)
      if (!f.exists) {
        throw new PdfConversionException("no xml file to parse found: " + outFilePsToText)
      }
      S.set("state","uploading")
      S.set("message","40%: running metatagger (may take several minutes)")
      S.set("percentage","40")

      val resRuncrf = (sys.process.Process(Seq("echo", outFilePsToText + " -> " + outFileRunCrf)) #| sys.process.Process("bin/runcrf", new java.io.File(runcrfFilePath))).!!

      props.addOrReplaceValue(propertiesFilename,"ispdfalreadyparsed","true")
  }

  val outfiles: Map[String, PageXml] =
  {
    Map("metatagger" -> new PageXml("metatagger", File(outFileRunCrf)))
  }
  pagexmls.set(outfiles)
}

class PsToTextConversionException(s: String) extends Exception(s)
