package edu.umass.cs.iesl.pdf2meta.webapp.cakesnippet

import xml.{Text, NodeSeq}
import net.liftweb.util.BindHelpers._
import net.liftweb.common.{Full, Box}
import net.liftweb.http.js.JsCmds._
import net.liftweb.http.js.JsCmd
import scala.Predef._
import edu.umass.cs.iesl.pdf2meta.webapp.lib.PdfToJpg
import edu.umass.cs.iesl.pdf2meta.cli.coarsesegmenter._
import edu.umass.cs.iesl.pdf2meta.cli.WebPipelineComponent
import edu.umass.cs.iesl.pdf2meta.cli.extract.XmlExtractorComponent
import edu.umass.cs.iesl.pdf2meta.cli.layoutmodel._
import edu.umass.cs.iesl.pdf2meta.cli.util.Workspace

import CoarseSegmenterTypes._
import collection.Seq


trait ShowPdfComponent
  {
  this: WebPipelineComponent with XmlExtractorComponent with
          CoarseSegmenterComponent =>

  object ShowPdf extends Function1[NodeSeq, NodeSeq]
    {
    def apply(in: NodeSeq): NodeSeq =
      {
      val w = new Workspace(filenameBox.get.openTheBox, filestreamBox.get.openTheBox)

      val length: Box[Text] = Full(Text(w.file.length.toString))

      val filename: Box[Text] = Full(Text(w.filename))

      def bindPdfInfo(template: NodeSeq): NodeSeq =
        {
        val pdfToJpg: PdfToJpg = new PdfToJpg(w) // stores images in session as side effect
        val (doc: DocNode, classifiedRectangles: ClassifiedRectangles) = pipeline.apply(w)

        val allDelimitingBoxes: Seq[DelimitingBox] = doc.delimitingBoxes

        val allTextBoxes: Seq[DocNode] = doc.textBoxChildren

        def bindPage(pageTemplate: NodeSeq): NodeSeq =
          {


          val leafRects: Seq[RectangleOnPage] = doc.allLeaves.map(_.rectangle).flatten
          val pages = leafRects.map(_.page).distinct.sortBy(_.pagenum)

          val boundPage = pages.flatMap
                          {page =>
                            {
                            val rectangles: ClassifiedRectangles = classifiedRectangles.onPage(page)
                            val legitNonRedundant: Seq[CoarseSegmenterTypes.ClassifiedRectangle] = rectangles.legitNonRedundant
                            val discarded: Seq[CoarseSegmenterTypes.ClassifiedRectangle] = rectangles.discarded

                            val image = pageimages.get(page.pagenum).imageUrl

                            val delimitingBoxes = allDelimitingBoxes.filter(_.rectangle match
                                                                            {
                                                                              case Some(x) => x.page == page;
                                                                              case None => false;
                                                                            })
                            val textBoxes = allTextBoxes.filter(_.rectangle match
                                                                {
                                                                  case Some(x) => x.page == page;
                                                                  case None => false;
                                                                })

                            bind("page", pageTemplate, AttrBindParam("id", page.pagenum.toString, "id"), "image" -> image, "segments" -> bindSegment(legitNonRedundant) _,
                                 "textboxes" -> bindTextBoxes(textBoxes) _, "rectboxes" -> bindDelimitingBoxes(delimitingBoxes) _, "discardboxes" -> bindDiscards(discarded.map(_._1)) _,
                                 "readingorder" ->
                                 bindReadingOrder(page, ReadingOrderPair.joinPairs(rectangles.raw.map(_._1).toList)) _)
                            };
                          }
          boundPage
          }


        bind("pdfinfo", template, "filename" -> Text(w.file.toString()), "successbox" -> bindSuccessBox(doc.info) _, "errorbox" -> bindErrorBox(doc.errors) _, "pages" -> bindPage _)
        }


      bind("ul", in, "file_name" -> filename,
           // "mime_type" -> mimetype, // Text(v.mimeType)),
           "length" -> length, //"md5" -> md5str,
           "pdfinfo" -> bindPdfInfo _)
      }


    private def bindSegment(segments: Seq[ClassifiedRectangle])(segmentTemplate: NodeSeq): NodeSeq =
      {
      segments.flatMap
      {
      case (textbox: DocNode, classification, features, scores) =>
        {
        //val details = "\"" + features.mkString("<br/>") + "<hr/>" + scores.mkString("<br/>" + "\"")
        val details = features.mkString("<br/>") + "<hr/>" + scores.mkString("<br/>")


        val truncatedText: String =
          {
          val s = textbox.text.substring(0,textbox.text.length.min(200))
          s.length match
          {
            case 200 => s + " ..."
            case _ => s
          }
          }

        bind("segment", segmentTemplate, "classification" -> classification, "text" ->
                                                                             truncatedText, FuncAttrBindParam("onmouseover", (ns: NodeSeq) =>
          {
          //Script(new JsCmd
          //                       {def toJsCmd = ("tooltip.show(\"bogus\");")}) //" + details + "
          Text("tooltip.show('bogus'); return true")
          }, "onmouseover"),
             //AttrBindParam("style", "border: green 2px solid; visibility: invisible; ", "style"),
             FuncAttrBindParam("class", (ns: NodeSeq) => addId(textbox, ns), "class"))
        }
      case _ => NodeSeq.Empty
      }
      }

    private def bindTextBoxes(textBoxes: Seq[DocNode])(textboxTemplate: NodeSeq): NodeSeq =
      {
      textBoxes.flatMap
      {textbox =>
        {
        bind("textbox", textboxTemplate, FuncAttrBindParam("style", (ns: NodeSeq) => addCoords(textbox, ns), "style"), FuncAttrBindParam("class", (ns: NodeSeq) => addId(textbox, ns), "class"))
        }
      }
      }
    private def bindDelimitingBoxes(rectBoxes: Seq[DelimitingBox])(rectboxTemplate: NodeSeq): NodeSeq =
      {
      rectBoxes.flatMap
      {rectbox =>
        {
        bind("rectbox", rectboxTemplate, FuncAttrBindParam("style", (ns: NodeSeq) => addCoords(rectbox, ns), "style"), FuncAttrBindParam("class", (ns: NodeSeq) => addId(rectbox, ns), "class"))
        }
      }
      }
    // redundant code
    private def bindDiscards(rectBoxes: Seq[DocNode])(rectboxTemplate: NodeSeq): NodeSeq =
      {
      rectBoxes.flatMap
      {rectbox =>
        {
        bind("rectbox", rectboxTemplate, FuncAttrBindParam("style", (ns: NodeSeq) => addCoords(rectbox, ns), "style"), FuncAttrBindParam("class", (ns: NodeSeq) => addId(rectbox, ns), "class"))
        }
      }
      }

    private def bindReadingOrder(page: Page, readingorder: Seq[ReadingOrderPair])(readingOrderTemplate: NodeSeq): NodeSeq =
      {

      val lines = (for (r <- readingorder) yield
        {
        ("document.getElementById(" + page.pagenum + ").appendChild(createLine(" + r.x1 + ", " +
         (page.rectangle.height - r.y1) + ", " + r.x2 + ", " + (page.rectangle.height - r.y2) +
         ")); ")
        }).mkString("\n")

      Script(new JsCmd
        {
        def toJsCmd = lines
        })
      }


    private def bindErrorBox(errors: Option[Iterator[String]])(errorboxTemplate: NodeSeq): NodeSeq =
      {
      def bindErrorLine(errors: Iterator[String])(errorlineTemplate: NodeSeq): NodeSeq =
        {
        (errors.flatMap
         {text => bind("error", errorlineTemplate, "text" -> text)}).toSeq
        }

      errors match
      {
        case None => NodeSeq.Empty
        case Some(x) => bind("errorbox", errorboxTemplate, "errors" -> bindErrorLine(x) _);
      }
      }

    private def bindSuccessBox(info: Option[Iterator[String]])(successboxTemplate: NodeSeq): NodeSeq =
      {
      def bindSuccessLine(success: Iterator[String])(successlineTemplate: NodeSeq): NodeSeq =
        {
        (success.flatMap
         {text => bind("success", successlineTemplate, "text" -> text)}).toSeq
        }

      info match
      {
        case None => NodeSeq.Empty
        case Some(x) => bind("successbox", successboxTemplate, "successes" -> bindSuccessLine(x) _);
      }
      }


    private def addId(r: DocNode, ns: NodeSeq): NodeSeq = Text(r.id + " ") ++ ns
    private def addCoords(r: DocNode, ns: NodeSeq): NodeSeq =
      {
      val rr: RectangleOnPage = r.rectangle.get
      Text("position: absolute; top: " +
           (rr.page.rectangle.height - rr.top) +
           "px; left: " + rr.left +
           "px; width: " + rr.width +
           "px; height: " +
           rr.height + "px; ") ++ ns
      }
    }

  }
