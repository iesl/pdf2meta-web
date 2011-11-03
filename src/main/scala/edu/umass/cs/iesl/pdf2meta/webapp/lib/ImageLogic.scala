package edu.umass.cs.iesl.pdf2meta.webapp.lib

import net.liftweb._
import common._
import net.liftweb.util.Helpers._

import http._
import tools.nsc.io.File
import scalax.io.Resource
import xml.NodeSeq
import edu.umass.cs.iesl.pdf2meta.webapp.cakesnippet.pageimages

object ImageLogic
  {

  object TestImage
    {
    def unapply(in: String): Option[PageImage] =
      {
      val images = pageimages.get
      val pageno = in.trim.toInt
      try
      {
      Full(images(pageno))
      }
      catch
      {
      case e => Empty
      }
      }
    }

  def matcher: LiftRules.DispatchPF =
    {
    case req@Req("image" :: TestImage(img) :: Nil, _, GetRequest) =>
      () => serveImage(img, req)
    }
  def serveImage(img: PageImage, req: Req): Box[LiftResponse] =
    {
    Full(InMemoryResponse(img.bytes, List("Last-Modified" -> img.date, "Content-Type" -> "image/jpg", "Content-Length" -> img.length), Nil /*cookies*/ , 200))
    }
  }

case class PageImage(pageid: Int, file: File, mimeType: String)
  {
  val bytes: Array[Byte] = Resource.fromInputStream(file.inputStream()).byteArray
  val date = toInternetDate(file.lastModified)
  val length = file.length.toString

  def imageUrl: NodeSeq =
    {
      <img src={"/image/" + pageid}/>
    }
  }

