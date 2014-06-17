package edu.umass.cs.iesl.pdf2meta.webapp.lib

import sys.process.{ProcessIO, Process}
import com.typesafe.scalalogging.slf4j.Logging
import scala.None
import edu.umass.cs.iesl.pdf2meta.webapp.cakesnippet.pageimages
import edu.umass.cs.iesl.scalacommons.Workspace
import net.liftweb.http.RequestVar
import net.liftweb.common.{Box, Empty}

import scala.Some

//import org.scala_tools.subcut.inject.AutoInjectable
import com.escalatesoft.subcut.inject.{Injectable, BindingModule}

import tools.nsc.io.File
import net.liftweb._
import http._

import _root_.net.liftweb._

/*
 * Created by IntelliJ IDEA.
 * User: lorax
 * Date: 9/2/11
 * Time: 11:50 AM
 */
class PdfToJpg(w: Workspace)(implicit val bindingModule: BindingModule) extends Logging with Injectable
	{
  object loginType extends RequestVar[Box[String]](Empty)
	val outfilebase = w.dir + "/" + w.filename + ".jpg";

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
  //gets the dimension of pdf first page, assuming that the rest of the pages have the same size

  val identifyPath = inject[String]('identify)
  val commandIdentify = identifyPath + " " + w.file

  val outputIdentify =runCommand(commandIdentify)

  val reg = new scala.util.matching.Regex(""" ([\d]+)x([\d]+) """, "width", "height")

  val mch = reg.findAllIn(outputIdentify.toString)

  val height={if(!mch.isEmpty){mch group "height"}else{"792"}}
  val width={if(!mch.isEmpty){mch group "width"}else{"612"}}

  S.set("width",width)
  S.set("height",height)

 	val convertPath = inject[String]('convert)
	val command = convertPath + " -density 400 -verbose " + w.file + " " + outfilebase


  val output = runCommand(command)

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
			Some(pageid -> new PageImage(pageid, f, "image/jpg", S.get("width").openOrThrowException("err in width"),
        S.get("height").openOrThrowException("err in height")))
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
			pageimages.set(Map((1, new PageImage(1, f, "image/jpg", S.get("width").openOrThrowException("err in width"),
                S.get("height").openOrThrowException("err in height")))))
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
