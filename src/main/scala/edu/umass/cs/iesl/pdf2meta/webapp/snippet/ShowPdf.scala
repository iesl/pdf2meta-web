package edu.umass.cs.iesl.pdf2meta.webapp.snippet

import xml.{Text, NodeSeq}
import net.liftweb.util.BindHelpers._
import java.io.InputStream
import net.liftweb.common.{Empty, Full, Box}
import net.liftweb.http.SessionVar
import net.liftweb.http.js.JsCmds._
import net.liftweb.http.js.JsCmd
import scala.Predef._
import edu.umass.cs.iesl.pdf2meta.cli.util.Workspace
import edu.umass.cs.iesl.pdf2meta.webapp.lib.{PdfToJpg, PageImage}
import edu.umass.cs.iesl.pdf2meta.cli.extract.PdfMiner
import edu.umass.cs.iesl.pdf2meta.cli.layout.{RectBox, TextBox, Page, LayoutRectangle}
import edu.umass.cs.iesl.pdf2meta.cli.eval.{CoarseSegmenterTypes, ClassifiedRectangles, ScoringModel, CoarseSegmenter}

/*
 * Created by IntelliJ IDEA.
 * User: lorax
 * Date: 9/8/11
 * Time: 10:20 AM
 */
object filestreamBox extends SessionVar[Box[InputStream]](Empty);

object filenameBox extends SessionVar[Box[String]](Empty);

object pageimages extends SessionVar[Map[Int, PageImage]](null);


case class ReadingOrderPair(x1: Double, y1: Double, x2: Double, y2: Double);


class ShowPdf
  {
  private def joinPairs(list: List[LayoutRectangle]): List[ReadingOrderPair] =
    {
    // require list is sorted and nonoverlapping
    list match
    {
      case Nil => Nil
      case a :: t =>
        t match
        {
          case Nil => List(new ReadingOrderPair(a.rectangle.horizontalMiddle, a.rectangle.top - 3,
                                                a.rectangle.horizontalMiddle, a.rectangle.bottom + 3))
          case _ => List(new ReadingOrderPair(a.rectangle.horizontalMiddle, a.rectangle.top - 3,
                                              a.rectangle.horizontalMiddle, a.rectangle.bottom + 3), new
                          ReadingOrderPair(a.rectangle.horizontalMiddle, a.rectangle.bottom + 3,
                                           t.head.rectangle.horizontalMiddle, t.head.rectangle.top - 3)) :::
                    joinPairs(t)
        }
      // assert(b._1 > a._2)
    }
    }

  def render(in: NodeSeq): NodeSeq =
    {
    //    val box: Box[FileParamHolder] = theUpload.is
    //  val v = box.openTheBox
    /*  val mimetype: Box[Text] = v.mimeType match
    {
      case null => Full(Text("No mime type supplied"))
      case _ => Full(Text(v.mimeType))
    }
*/
    val w = new Workspace(filenameBox.get.openTheBox, filestreamBox.get.openTheBox)

    //Box.legacyNullTest(v.mimeType).map(Text).openOr(Text("No mime type supplied"))
    val length: Box[Text] = Full(Text(w.file.length.toString))
    // val md5str: Box[Text] = Full(Text(hexEncode(md5(w.file))))
    def bindPdfInfo(template: NodeSeq): NodeSeq =
      {
      val pdfToJpg: PdfToJpg = new PdfToJpg(w) // stores images in session as side effect
      val pdfMiner: PdfMiner = new PdfMiner(w)

      val segments = new CoarseSegmenter(pdfMiner.pages, ScoringModel.scoringFunctions)

      //val classifiedBoxes = CoarseSegmenter.classifiedBoxes(pdfMiner.textBoxes)
      val pages = pdfMiner.pages


            def addId(r:LayoutRectangle, ns: NodeSeq): NodeSeq = Text(r.id + " ") ++ ns
       def addCoords(page:Page,r:LayoutRectangle,ns: NodeSeq): NodeSeq =
              {
              Text("position: absolute; top: " +
                   (page.rectangle.height - r.rectangle.top) +
                   "px; left: " + r.rectangle.left +
                   "px; width: " + r.rectangle.width +
                   "px; height: " +
                   r.rectangle.height + "px; ") ++ ns
              }

      def bindPage(pageTemplate: NodeSeq): NodeSeq =
        {

        /*def bindTextLine(textLines: Seq[Tuple3[Int, String, String]])(textlineTemplate: NodeSeq): NodeSeq =
          {
          textLines.flatMap
          {
          case (lineno, linetype, text) => bind("textline", textlineTemplate, "number" ->
                                                                              lineno, "linetype" ->
                                                                                      linetype, "text" ->
                                                                                                text);
          }
          }*/
        def bindSegment(segments: Seq[Tuple2[LayoutRectangle, String]])(segmentTemplate: NodeSeq): NodeSeq =
          {
          segments.flatMap
          {
          case (textbox: TextBox, classification) =>
            {
            bind("segment", segmentTemplate, "classification" ->
                                             classification, "text" ->
                                                             textbox.text,
                 //AttrBindParam("style", "border: green 2px solid; visibility: invisible; ", "style"),
                 FuncAttrBindParam("class", (ns:NodeSeq)=>addId(textbox,ns), "class"))
            }
          case _ => NodeSeq.Empty
          }
          }

        def bindTextBoxes(page: Page, textBoxes: Seq[LayoutRectangle])(textboxTemplate: NodeSeq): NodeSeq =
          {
          textBoxes.flatMap
          {textbox =>
            {

            bind("textbox", textboxTemplate, FuncAttrBindParam("style", (ns:NodeSeq)=>addCoords(page,textbox,ns), "style"),
                 FuncAttrBindParam("class", (ns:NodeSeq)=>addId(textbox,ns), "class"))
            }
          }
          }
        def bindRectBoxes(page: Page, rectBoxes: Seq[RectBox])(rectboxTemplate: NodeSeq): NodeSeq =
          {
          rectBoxes.flatMap
          {rectbox =>
            {
            bind("rectbox", rectboxTemplate, FuncAttrBindParam("style", (ns:NodeSeq)=>addCoords(page,rectbox,ns), "style"),
                 FuncAttrBindParam("class", (ns:NodeSeq)=>addId(rectbox,ns), "class"))
            }
          }
          }
        // redundant code
        def bindDiscards(page: Page, rectBoxes: Seq[LayoutRectangle])(rectboxTemplate: NodeSeq): NodeSeq =
          {
          rectBoxes.flatMap
          {rectbox =>
            {
            bind("rectbox", rectboxTemplate, FuncAttrBindParam("style", (ns:NodeSeq)=>addCoords(page,rectbox,ns), "style"),
                 FuncAttrBindParam("class", (ns:NodeSeq)=>addId(rectbox,ns), "class"))
            }
          }
          }

        def bindReadingOrder(page: Page, readingorder: Seq[ReadingOrderPair])(readingOrderTemplate: NodeSeq): NodeSeq =
          {
          val lines = (for (r <- readingorder) yield
            {
            ("document.getElementById(" + page.pagenumber + ").appendChild(createLine(" + r.x1 + ", " +
             (page.rectangle.height - r.y1) + ", " + r.x2 + ", " + (page.rectangle.height - r.y2) +
             ")); ")
            }).mkString("\n")

          Script(new JsCmd
            {
            def toJsCmd = lines
            })
          }

        def bindErrorLine(errors: Seq[String])(errorlineTemplate: NodeSeq): NodeSeq =
          {
          errors.flatMap
          {text => bind("error", errorlineTemplate, "text" -> text);
          }
          }

        def bindErrorBox(errors: Seq[String])(errorboxTemplate: NodeSeq): NodeSeq =
          {
          if (!errors.isEmpty)
            {
            bind("errorbox", errorboxTemplate, "errors" -> bindErrorLine(errors) _);
            }
          else NodeSeq.Empty
          }

        val res = pages.sorted.flatMap
                  {page =>
                    {
                    val rectangles: ClassifiedRectangles = segments.onPage(page.pagenumber).get
                    val legitNonRedundant: Seq[CoarseSegmenterTypes.ClassifiedRectangle] = rectangles.legitNonRedundant
                    val discarded: Seq[CoarseSegmenterTypes.ClassifiedRectangle] = rectangles.discarded

                    bind("page", pageTemplate, AttrBindParam("id", page.id, "id"),
                         "image" -> pageimages.get(page.pagenumber).imageUrl,
                         "segments" -> bindSegment(legitNonRedundant) _,
                         "textboxes" -> bindTextBoxes(page, page.textBoxes) _,
                         "rectboxes" -> bindRectBoxes(page, page.rects) _,
                         "discardboxes" -> bindDiscards(page, discarded.map(_._1)) _,
                         "readingorder" -> bindReadingOrder(page, joinPairs(legitNonRedundant.map(_._1).toList)) _,
                         "errorbox" -> bindErrorBox(page.errors) _)
                    };
                  }
        res
        }



      //def bindAll(template: NodeSeq): NodeSeq =
      //{
      bind("pdfinfo", template, "filename" -> Text(w.file.toString()), "command" -> pdfMiner.command,
           "output" -> pdfMiner.output, "pages" -> bindPage _)
      //  }
      //val result = Text(pdfInfo.toString)
      // pdfInfo.clean
      // result
      //bindAll
      }
    val filename: Box[Text] = Full(Text(w.filename))

    bind("ul", in, "file_name" -> filename,
         // "mime_type" -> mimetype, // Text(v.mimeType)),
         "length" -> length, //"md5" -> md5str,
         "pdfinfo" -> bindPdfInfo _)
    }
  }
