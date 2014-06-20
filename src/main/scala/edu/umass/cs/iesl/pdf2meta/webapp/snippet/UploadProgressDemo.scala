package edu.umass.cs.iesl.pdf2meta.webapp.snippet

import net.liftweb.http._
import net.liftweb.common.{Full, Empty, Box}
//import
import scala.xml.{Text, NodeSeq}
import net.liftweb.util.Helpers._
import edu.umass.cs.iesl.pdf2meta.webapp.ajax.UploadProgress

/**
 * Created by klimzaporojets on 6/19/14.
 */
/**
 * This is a pure example class - nothing more.
 * Use this as a base for implementing the widget
 * in your own applications.
 */
object UploadProgressDemo extends DispatchSnippet {

  private object theUpload extends RequestVar[Box[FileParamHolder]](Empty)

  def dispatch = {
    case "upload" => upload _
    case "script" => UploadProgress.head _
  }

  def script(xhtml: NodeSeq):NodeSeq =
  {
    UploadProgress.head(xhtml)
  }
  def upload(xhtml: NodeSeq): NodeSeq = {
    if (S.get_?){
      bind("ul", chooseTemplate("choose", "get", xhtml),
        "file_upload" -> SHtml.fileUpload(ul => theUpload(Full(ul))))
    } else {
      bind("ul", chooseTemplate("choose", "post", xhtml),
        "file_name" -> theUpload.is.map(v => Text(v.fileName)),
        "mime_type" -> theUpload.is.map(v => Box.legacyNullTest(v.mimeType).map(s => Text(s)).openOr(Text("No mime type supplied"))),
        "length" -> theUpload.is.map(v => Text(v.file.length.toString)),
        "md5" -> theUpload.is.map(v => Text(hexEncode(md5(v.file))))
      )
    }
  }
}
