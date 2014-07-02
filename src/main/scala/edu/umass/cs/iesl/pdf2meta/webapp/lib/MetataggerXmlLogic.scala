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
