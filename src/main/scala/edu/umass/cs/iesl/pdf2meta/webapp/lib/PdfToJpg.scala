package edu.umass.cs.iesl.pdf2meta.webapp.lib

import sys.process.{ProcessIO, Process}
import com.typesafe.scalalogging.slf4j.Logging
import scala.None
import edu.umass.cs.iesl.pdf2meta.webapp.cakesnippet.pageimages
import edu.umass.cs.iesl.scalacommons.Workspace
import net.liftweb.http.RequestVar
import net.liftweb.common.{Box, Empty}

import scala.Some
import scala.reflect.io.Directory

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


//  object loginType extends RequestVar[Box[String]](Empty)
  val props:MapToProperties = new MapToProperties()
  val propertiesFilename:String =
    S.get("propertiesFile").openOrThrowException("err while obtaining properties file")

  val properties:Map[String,String] = props.readPropertiesFile(propertiesFilename)

  def imageAlreadyExists() = {
    ((properties.get("imagedir")!=None) && !(properties.get("width")==None) && !(properties.get("height") == None))
  }

  def getUpdatedProperties(properties:Map[String,String]):Map[String,String] =
  {
    if(!imageAlreadyExists) {
      val outfilebase = w.dir + "/" + w.filename + ".jpg";

      //gets the dimension of pdf first page, assuming that the rest of the pages have the same size

      val identifyPath = inject[String]('identify)
      val commandIdentify = identifyPath + " " + w.file

      val outputIdentify = linuxCommandExecuter.runCommand(commandIdentify)

      val reg = new scala.util.matching.Regex( """ ([\d]+)x([\d]+) """, "width", "height")

      val mch = reg.findAllIn(outputIdentify.toString)

      val height = {
        if (!mch.isEmpty) {
          mch group "height"
        } else {
          "792"
        }
      }
      val width = {
        if (!mch.isEmpty) {
          mch group "width"
        } else {
          "612"
        }
      }

//      S.set("width", width)
//      S.set("height", height)
      props.addOrReplaceValue(propertiesFilename,"width",width)
      props.addOrReplaceValue(propertiesFilename,"height",height)
      props.addOrReplaceValue(propertiesFilename,"imagedir",w.dir.path)

      val convertPath = inject[String]('convert)
      val command = convertPath + " -density 400 -verbose " + w.file + " " + outfilebase


      val output = linuxCommandExecuter.runCommand(command)
      props.readPropertiesFile(propertiesFilename)
    }
    else
    {
      properties
    }
  }
  //----here ends the image extraction

  val updatedProperties:Map[String,String] = getUpdatedProperties(properties)

	val outfiles: Map[Int, PageImage] =
		{
		val pageidRE = "-(\\d+)\\.jpg".r
    val imDir = Directory(updatedProperties.get("imagedir").get)
		val elements: Iterator[Option[(Int, PageImage)]] = for (f <- imDir.files /*w.dir.files*/) yield
			{
			try
			{
			val r = pageidRE findFirstIn f.segments.last;
			val pageidRE(pageidStr) = r.getOrElse()
			val pageid = pageidStr.toInt + 1
			Some(pageid -> new PageImage(pageid, f, "image/jpg", updatedProperties.get("width").get /*S.get("width").openOrThrowException("err in width")*/,
          updatedProperties.get("height").get)/*S.get("height").openOrThrowException("err in height"))*/)
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
			pageimages.set(Map((1, new PageImage(1, f, "image/jpg", updatedProperties.get("width").get
			          /*S.get("width").openOrThrowException("err in width")*/,
                updatedProperties.get("height").get
                /*S.get("height").openOrThrowException("err in height")*/))))
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
