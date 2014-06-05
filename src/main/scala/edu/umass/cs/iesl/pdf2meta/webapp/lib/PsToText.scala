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


  println(inject[String]('examples))

  val filePath = inject[String]('pstotext_path)

  val outfilebase = filePath + "/" + w.filename + ".xml";

  val outfileruncrfbase = filePath + "/" + w.filename + "_runcrf.xml";
  val convertPath:String = inject[String]('pstotext)

  val runcrfFilePath = inject[String]('runcrf_path)

  val output =
  {
    try
    {
    val result = (convertPath + " " + w.file).toString #> new java.io.File(outfilebase) !

    if(result!=0)
    {
      throw new PdfConversionException("Error while executing pstotext")
    }
        result
    }catch
    {
      case e: Exception => println("exception caught: " + e);
    }

  }
  val f = new java.io.File(outfilebase)
  if(!f.exists)
  {
    throw new PdfConversionException("no xml file to parse found: " + outfilebase)
  }

//  sys.process.Process(Seq("sbt", "update"), new java.io.File("/path/to/project")).!!
  val resRuncrf = (sys.process.Process(Seq("echo", outfilebase + " -> " + outfileruncrfbase)) #| sys.process.Process("bin/runcrf", new java.io.File(runcrfFilePath))).!!

  println(resRuncrf)
  //now invokes metatagger

}

class PsToTextConversionException(s: String) extends Exception(s)


/*

import sys.process.{ProcessIO, Process}
import com.typesafe.scalalogging.slf4j.Logging
import scala.None
import edu.umass.cs.iesl.pdf2meta.webapp.cakesnippet.pageimages
import edu.umass.cs.iesl.scalacommons.Workspace
//import org.scala_tools.subcut.inject.AutoInjectable
import com.escalatesoft.subcut.inject.{Injectable, BindingModule}

import tools.nsc.io.File

/*
 * Created by IntelliJ IDEA.
 * User: lorax
 * Date: 9/2/11
 * Time: 11:50 AM
 */
class PdfToJpg(w: Workspace)(implicit val bindingModule: BindingModule) extends Logging with Injectable
	{

	val outfilebase = w.dir + "/" + w.filename + ".jpg";
	// w.dir + File.separator + w.file.segments.last + ".jpg"
	// ** make sure ImageMagick stuff is in the path
	// /usr/local/bin/convert
	val convertPath = inject[String]('convert)
	val command     = convertPath + " -verbose " + w.file + " " + outfilebase

	val output =
		{
		logger.info("Running " + command)
		val pb = Process(command)
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
		logger.info("Finished running (" + exitCode + ") " + command)
		output
		}


	val outfiles: Map[Int, PageImage] =
		{
		val pageidRE = "-(\\d+)\\.jpg".r

		val elements: Iterator[Option[(Int, PageImage)]] = for (f <- w.dir.files) yield
			{
			try
			{
			val r = pageidRE findFirstIn f.segments.last;
			val pageidRE(pageidStr) = r.getOrElse()
			val pageid = pageidStr.toInt + 1
			Some(pageid -> new PageImage(pageid, f, "image/jpg"))
			}
			catch
			{
			case e: MatchError =>
				{
				logger.error("Image name match failed: " + f.segments.last)
				None
				} // ignore
			}
			}
		elements.flatten.toMap
		}

	if (outfiles.isEmpty)
		{
		// probably a one-page document
		val f = File(w.file.toString() + ".jpg")
		if (f.exists)
			{
			pageimages.set(Map((1, new PageImage(1, f, "image/jpg"))))
			}
		else
			{
			throw new PdfConversionException("no page images found")
			}
		}
	else
		{
		pageimages.set(outfiles)
		}
	// pageimages.set(for ((f, pageno) <- outfiles.zipWithIndex) yield new PageImage(pageno, f, "image/jpg"))
	}

class PdfConversionException(s: String) extends Exception(s)

* */