package edu.umass.cs.iesl.pdf2meta.webapp.lib

import sys.process.{ProcessIO, Process}
import com.weiglewilczek.slf4s.Logging
import scala.None
import edu.umass.cs.iesl.pdf2meta.webapp.cakesnippet.pageimages
import edu.umass.cs.iesl.scalacommons.Workspace

/*
 * Created by IntelliJ IDEA.
 * User: lorax
 * Date: 9/2/11
 * Time: 11:50 AM
 */
class PdfToJpg(w: Workspace) extends Logging {

  val outfilebase = w.dir + "/" + w.filename + ".jpg";
  // w.dir + File.separator + w.file.segments.last + ".jpg"
  val command = "/usr/local/bin/convert -verbose " + w.file + " " + outfilebase

  val output = {
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

  /*  lazy val outfiles: Map[Int, PageImage] =
  {
  output // just trigger the lazy evaluation
  val result = collection.mutable.Map[Int, PageImage]()

  val pageidRE = "-(\\d+)\\.jpg".r

  for (f <- w.dir.files)
    {
    try
    {
    val r = pageidRE findFirstIn f.segments.last;
    r match
    {
      case None =>
      case Some(x) =>
        {
        logger.debug("Found " + x)
        val pageidRE(pageidStr) = x
        val pageid = pageidStr.toInt + 1
        logger.debug("Storing image mapping: " + pageid + " -> " + f)
        result += pageid -> new PageImage(pageid, f, "image/jpg")
        }
    }
    }
    catch
    {
    case e: MatchError =>
      {
      logger.error("Image name match failed: " + f.segments.last)
      } // ignore
    }
    }
  result.toMap
  }*/


  val outfiles: Map[Int, PageImage] = {
    val pageidRE = "-(\\d+)\\.jpg".r

    val elements: Iterator[Option[(Int, PageImage)]] = for (f <- w.dir.files) yield {
      try {
        val r = pageidRE findFirstIn f.segments.last;
        val pageidRE(pageidStr) = r.getOrElse()
        val pageid = pageidStr.toInt + 1
        Some(pageid -> new PageImage(pageid, f, "image/jpg"))
      }
      catch {
        case e: MatchError => {
          logger.error("Image name match failed: " + f.segments.last)
          None
        } // ignore
      }
    }
    elements.flatten.toMap
  }

  pageimages.set(outfiles)
  // pageimages.set(for ((f, pageno) <- outfiles.zipWithIndex) yield new PageImage(pageno, f, "image/jpg"))
}
