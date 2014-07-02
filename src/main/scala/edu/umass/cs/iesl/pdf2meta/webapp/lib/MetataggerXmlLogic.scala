package edu.umass.cs.iesl.pdf2meta.webapp.lib

import net.liftweb._
import common._
import net.liftweb.util.Helpers._

import http._
import tools.nsc.io.File
import scalax.io.Resource
import xml.NodeSeq
import edu.umass.cs.iesl.pdf2meta.webapp.cakesnippet.pagexmls
/**
 * Created by klimzaporojets on 6/20/14.
 */
object MetataggerXmlLogic {
  object TestXml
  {
    def unapply(in: String): Option[PageXml] =
    {
      val xmls = pagexmls.get
      val xmlId = in.trim //in.trim.toInt
      try
      {
        Full(xmls(xmlId))
      }
      catch
      {
        case e => Empty
      }
    }
  }
  def matcher: LiftRules.DispatchPF =
  {
    case req@Req("xml" :: TestXml(xml) :: Nil, _, GetRequest) =>
      () => serveXml(xml, req)
  }
  def serveXml(xml: PageXml, req: Req): Box[LiftResponse] =
  {
    Full(InMemoryResponse(xml.bytes, List("Last-Modified" -> xml.date, "Content-Type" -> "text/xml"/*"text/plain"*/, "Content-Length" -> xml.length), Nil /*cookies*/ , 200))
  }


  case class PageXml(xmlId: String, file: File /*, mimeType: String*/)
  {
    val bytes: Array[Byte] = Resource.fromInputStream(file.inputStream()).byteArray
    val date = toInternetDate(file.lastModified)
    val length = file.length.toString

    def xmlUrl: NodeSeq =
    {
        //<img src={"/image/" + pageid} width={"" + imwidth} height={"" + imheight}/>
      <a target={"_blank"} href = {"/xml/" + xmlId}>{"View the " + xmlId + " xml file"}</a>
    }
  }
}
/*
*

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

case class PageImage(pageid: Int, file: File, mimeType: String, imwidth:String, imheight:String)
  {
  val bytes: Array[Byte] = Resource.fromInputStream(file.inputStream()).byteArray
  val date = toInternetDate(file.lastModified)
  val length = file.length.toString

  def imageUrl: NodeSeq =
    {
      <img src={"/image/" + pageid} width={"" + imwidth} height={"" + imheight}/>
    }
  }
* */