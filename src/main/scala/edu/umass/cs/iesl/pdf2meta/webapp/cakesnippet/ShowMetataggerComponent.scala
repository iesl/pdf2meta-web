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
import collection.Seq
import java.util.Date
import edu.umass.cs.iesl.scalacommons.StreamWorkspace
import edu.umass.cs.iesl.pdf2meta.cli.extract.metatagger.MetataggerBoxTextAtom

//import org.scala_tools.subcut.inject.AutoInjectable
import com.escalatesoft.subcut.inject.{Injectable, BindingModule}

import edu.umass.cs.iesl.pdf2meta.cli.layoutmodel._
import java.io.FileInputStream

//kzaporojets: modified for metatagger
trait ShowMetataggerComponent
	{
	this: WebPipelineComponent =>

	// with XmlExtractorComponent with
	// CoarseSegmenterComponent =>
	class ShowMetatagger(implicit val bindingModule:BindingModule) extends (NodeSeq => NodeSeq) with Injectable
		{
		def apply(in: NodeSeq): NodeSeq =
			{
			// kzaporojets: comment because not compiling val w = new StreamWorkspace(filenameBox.get.openTheBox, filestreamBox.get.openTheBox)

      val w = new StreamWorkspace("output_pstotext_runcrf_v2.pdf", new FileInputStream("/Users/klimzaporojets/klim/pdf2meta/pdf2meta-web/examples/output_pstotext_runcrf_v2.pdf"))

      val w_xml = new StreamWorkspace("output_pstotext_runcrf_v3.xml", new FileInputStream("/Users/klimzaporojets/klim/pdf2meta/pdf2meta-web/examples/output_pstotext_runcrf_v3.xml"))


        //use in future when integrated with the initial webpage
        //new StreamWorkspace(filenameBox.get.openOrThrowException("exception") , filestreamBox.get.openOrThrowException("exception"))

			val length: Box[Text] = Full(Text(w.file.length.toString))

			val filename: Box[Text] = Full(Text(w.filename))

			def bindPdfInfo(template: NodeSeq): NodeSeq =
				{

				logger.debug("Starting PDF image generation...")
				val startTime = new Date
				logger.debug("Running PDF image generation...")

				//** could use pdfbox converter here
				val pdfToJpg: PdfToJpg = new PdfToJpg(w) // stores images in session as side effect

				logger.debug("PDF image generation done ")
				val extractTime = new Date()
				logger.debug("PDF image generation took " + ((extractTime.getTime - startTime.getTime)) + " milliseconds")
//kzaporojets: todo change here to metataggerPipeline
//				val (doc: DocNode, classifiedRectangles: ClassifiedRectangles) = pipeline.apply(w)
        val (doc: DocNode, classifiedRectangles: ClassifiedRectangles) = metataggerPipeline.apply(w_xml)

  //metataggerPipeline.apply(w)

				logger.debug("Conversion pipeline done ")
				val pipelineTime = new Date()
				logger.debug("Conversion pipeline took " + ((pipelineTime.getTime - extractTime.getTime)) + " milliseconds")
//
				val allDelimitingBoxes: Seq[DelimitingBox] = doc.delimitingBoxes
				val allWhitespaceBoxes: Seq[WhitespaceBox] = doc.whitespaceBoxes

				val allTextBoxes: Seq[DocNode] = doc.children //textBoxChildren
				def bindPage(pageTemplate: NodeSeq): NodeSeq =
					{

					val leafRects: Seq[RectangleOnPage] = doc.leaves.map(_.rectangle).flatten
					val pages = leafRects.map(_.page).distinct.sortBy(_.pagenum)

					val boundPage = pages.flatMap
					                {
					                page =>
						                {
						                val rectangles: ClassifiedRectangles = classifiedRectangles.onPage(page)
//						                // val legitNonRedundant: Seq[ClassifiedRectangle] = rectangles.legitNonRedundant
						                val all: Seq[ClassifiedRectangle] = rectangles.raw
//						                //val legit: Seq[ClassifiedRectangle] = rectangles.legit
//						                val legit: Seq[ClassifiedRectangle] = rectangles.legit
//						                val discarded: Seq[ClassifiedRectangle] = rectangles.discarded

						                val image = pageimages.get(page.pagenum).imageUrl

						                val delimitingBoxes = allDelimitingBoxes.filter(_.rectangle match
						                                                                {
							                                                                case Some(x) => x.page == page;
							                                                                case None    => false;
						                                                                })
						                val whitespaceBoxes = allWhitespaceBoxes.filter(_.rectangle match
						                                                                {
							                                                                case Some(x) => x.page == page;
							                                                                case None    => false;
						                                                                })
						                val textBoxes = allTextBoxes.filter(_.rectangle match
						                                                    {
							                                                    case Some(x) => x.page == page;
							                                                    case None    => false;
						                                                    })

						                bind("page", pageTemplate,
						                     AttrBindParam("id", page.pagenum.toString, "id"),
						                     "image" -> image,
                                 "sidelabels" -> bindSidelabels(all) _,
//						                     "segments" -> bindSegment(all) _,
//						                     "features" -> bindFeatures(all) _,
						                     "textboxes" -> bindTextBoxes(textBoxes) _
                              //,
//						                     "delimitingboxes" -> bindDelimitingBoxes(delimitingBoxes) _
//						                     "whitespaceboxes" -> bindWhitespaceBoxes(textBoxes) _
//						                     "discardboxes" -> bindDiscardBoxes(discarded.map(_.node)) _,
//						                     "readingorder" -> bindReadingOrder(page, ReadingOrderPair.joinPairs(legit.map(_.node).toList)) _)
                            )
						                };
					                }
					boundPage
					}


				val result = bind("pdfinfo", template, "filename" -> Text(w.file.toString()), "successbox" -> bindSuccessBox(doc.info) _,
				                  "errorbox" -> bindErrorBox(doc.errors) _, "pages" -> bindPage _)

// kzaporojets: just comment
//				logger.debug("Clearing temporary directory " + w.dir)
//				w.clean()

				logger.debug("HTML Rendering done ")
				val renderTime = new Date()
				logger.debug("HTML Rendering took " + ((renderTime.getTime - pipelineTime.getTime)) + " milliseconds")
				result
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
			case x: ClassifiedRectangle =>
				{
				//val details = "\"" + features.mkString("<br/>") + "<hr/>" + scores.mkString("<br/>" + "\"")
				//val details = features.mkString("<br/>") + "<hr/>" + scores.mkString("<br/>")
				val truncatedText: String =
					{
					val t: String = x.node.text.trim
					val s = t.substring(0, t.length.min(200))
					s.length match
					{
						case 200 => s + " ..."
						case 0   => "EMPTY"
						case _   => s
					}
					}

				bind("segment", segmentTemplate, /*"classification" -> x.label.getOrElse("[none]"),*/ "text" ->
				                                                                                  truncatedText,

				     /*FuncAttrBindParam("onmouseover", (ns: NodeSeq) =>
							  {
							  //Script(new JsCmd
							  //                       {def toJsCmd = ("tooltip.show(\"bogus\");")}) //" + details + "
							  Text("tooltip.show('bogus'); return true")
							  }, "onmouseover"),
								 //AttrBindParam("style", "border: green 2px solid; visibility: invisible; ", "style"),
								 */
				     FuncAttrBindParam("class", (ns: NodeSeq) => (addId(x.node, ns) ++ Text((if (x.discarded) " discard" else ""))), "class"))
				}
			case _                      => NodeSeq.Empty
			}
			}



//    private def organizeLabels(sidelabels: Seq[ClassifiedRectangle], newSideLabels:Seq[ClassifiedRectangle]):Seq[ClassifiedRectangle] = {
//      val slabel: ClassifiedRectangle = sidelabels.head
//
//      val other = sidelabels.find(x=> ((slabel.node.rectangle.get.top > x.node.rectangle.get.bottom && slabel.node.rectangle.get.top < x.node.rectangle.get.top) ||
//        (slabel.node.rectangle.get.bottom > x.node.rectangle.get.bottom && slabel.node.rectangle.get.bottom < x.node.rectangle.get.top)))
//
//      //slabel.copy(node = slabel.node.copy() )
//
//      if(newSideLabels.exists(x => ((slabel.node.rectangle.get.top > x.node.rectangle.get.bottom && slabel.node.rectangle.get.top < x.node.rectangle.get.top) ||
//                                    (slabel.node.rectangle.get.bottom > x.node.rectangle.get.bottom && slabel.node.rectangle.get.bottom < x.node.rectangle.get.top)) ))
//      {
//        slabel+:sidelabels.tail
//        slabel.copy()
//      }
//
//
//    }


    private def getDistinctLabels(sidelabels:Seq[ClassifiedRectangle]):Seq[ClassifiedRectangle] = {
      if(sidelabels.size>1)
      {
        //sidelabels.tail.exists(x => x.node.id == sidelabels.head.node.id)
        val recValue = getDistinctLabels(sidelabels.tail)
        if(recValue.exists(x => x.node.id == sidelabels.head.node.id))
        {
          recValue
        }
        else
        {
          sidelabels.head +: recValue
        }
      }
      else
      {
        sidelabels;
      }
    }

    private def organizeLabels(sidelabels: Seq[ClassifiedRectangle]):Seq[ClassifiedRectangle] = {
      def compfn1 (classRectangle1:ClassifiedRectangle, classRectangle2:ClassifiedRectangle) = (classRectangle1.node.rectangle.get.top > classRectangle2.node.rectangle.get.top)
      val sortedLabels:Seq[ClassifiedRectangle] = sidelabels.sortWith(compfn1)
      //sortedLabels
      distributeLabels(sortedLabels,sortedLabels(0).node.rectangle.get.page.rectangle.height)
    }

    private def distributeLabels(sortedSideLabels: Seq[ClassifiedRectangle], yCoord:Float):Seq[ClassifiedRectangle] = {
      val headLabel:ClassifiedRectangle = sortedSideLabels.head



      /*
      new DelimitingBox((currentNode \ "@llx").text + (currentNode \ "@lly").text +
              (currentNode \ "@urx").text + (currentNode \ "@ury").text, new RectangleOnPage {
              override val page: Page = new Page(1,pageDimensions)
              override val bottom: Float = (currentNode \ "@lly").text.toFloat
              override val top: Float = (currentNode \ "@ury").text.toFloat
              override val left: Float = (currentNode \ "@llx").text.toFloat
              override val right: Float = (currentNode \ "@urx").text.toFloat
            }
      * */
      if(sortedSideLabels.size>1)
      {
        if(yCoord>headLabel.node.rectangle.get.top)
        {
          val distributedL:Seq[ClassifiedRectangle] = distributeLabels(sortedSideLabels.tail, headLabel.node.rectangle.get.top-25)
          headLabel.copy(node = new MetataggerBoxTextAtom(headLabel.node.id, headLabel.node.text.toUpperCase, "Font", 0.0f,
            new RectangleOnPage {override val page: Page = headLabel.node.rectangle.get.page
              override val bottom: Float = headLabel.node.rectangle.get.top + 23
              override val top: Float = headLabel.node.rectangle.get.top
              override val left: Float = headLabel.node.rectangle.get.left
              override val right: Float = headLabel.node.rectangle.get.right
            }, Array[Float](0f)))+:distributedL
        }
        else
        {
          val distributedL:Seq[ClassifiedRectangle] = distributeLabels(sortedSideLabels.tail, yCoord-25)
          headLabel.copy(node = new MetataggerBoxTextAtom(headLabel.node.id, headLabel.node.text.toUpperCase, "Font", 0.0f,
            new RectangleOnPage {override val page: Page = headLabel.node.rectangle.get.page
              override val bottom: Float = yCoord + 23
              override val top: Float = yCoord
              override val left: Float = headLabel.node.rectangle.get.left
              override val right: Float = headLabel.node.rectangle.get.right
            }, Array[Float](0f)))+:distributedL
        }
      }
      else
      {
        if(yCoord>headLabel.node.rectangle.get.top) {
          List(headLabel.copy(node = new MetataggerBoxTextAtom(headLabel.node.id, headLabel.node.text.toUpperCase, "Font", 0.0f,
            new RectangleOnPage {override val page: Page = headLabel.node.rectangle.get.page
              override val bottom: Float = headLabel.node.rectangle.get.top + 23
              override val top: Float = headLabel.node.rectangle.get.top
              override val left: Float = headLabel.node.rectangle.get.left
              override val right: Float = headLabel.node.rectangle.get.right
            }, Array[Float](0f))))
        }
        else
        {
          List(headLabel.copy(node = new MetataggerBoxTextAtom(headLabel.node.id, headLabel.node.text.toUpperCase, "Font", 0.0f,
            new RectangleOnPage {override val page: Page = headLabel.node.rectangle.get.page
              override val bottom: Float = yCoord + 23
              override val top: Float = yCoord
              override val left: Float = headLabel.node.rectangle.get.left
              override val right: Float = headLabel.node.rectangle.get.right
            }, Array[Float](0f))))
        }

      }
    }
    private def bindSidelabels(sidelabels: Seq[ClassifiedRectangle])(segmentTemplate: NodeSeq): NodeSeq =
    {
      organizeLabels(getDistinctLabels(sidelabels)).flatMap
      {
        case x: ClassifiedRectangle =>
        {
          //val details = "\"" + features.mkString("<br/>") + "<hr/>" + scores.mkString("<br/>" + "\"")
          //val details = features.mkString("<br/>") + "<hr/>" + scores.mkString("<br/>")
          val truncatedText: String =
          {
            val t: String = x.node.text.trim
            val s = t.substring(0, t.length.min(200))
            s.length match
            {
              case 200 => s + " ..."
              case 0   => "EMPTY"
              case _   => s
            }
          }

         // val res = addCoordsLabels(x.node)

          bind("sidelabel", segmentTemplate, "text" ->
            truncatedText,
            FuncAttrBindParam("class", (ns: NodeSeq) => (addId(x.node, ns) ++ Text((if (x.discarded) " discard" else ""))),
                    "class"),  FuncAttrBindParam("style", (ns: NodeSeq) => (addCoordsLabels(x.node, ns)), "style"))
          //val res = addCoordsLabels(x.node, ns)
        }
        case _                      => NodeSeq.Empty
      }
    }

		private def bindFeatures(segments: Seq[ClassifiedRectangle])(segmentTemplate: NodeSeq): NodeSeq =
			{
			segments.flatMap
			{
			case ClassifiedRectangle(textbox: DocNode, features, scores, Some(x)) =>
				{
				//val details = "\"" + features.mkString("<br/>") + "<hr/>" + scores.mkString("<br/>" + "\"")
				// val details = features.mkString("<br/>") + "<hr/>" + scores.mkString("<br/>")
				def bindPair(ss: Iterator[(String, Double)])(sTemplate: NodeSeq): NodeSeq =
					{
					(ss.flatMap
					 {
					 pair => bind("pair", sTemplate, "a" -> pair._1, "b" -> ("%3.2f" format pair._2))
					 }).toSeq
					}
				//.filter(_._2>0)
				bind("features", segmentTemplate, "features" -> bindPair(x.featureWeights.asSeq.map(f => (f._1.toString, f._2)).iterator) _,
				     "scores" -> bindPair(x.labelWeights.asSeq.iterator) _, FuncAttrBindParam("id", (ns: NodeSeq) => Text(textbox.id), "id"))
				}
			case ClassifiedRectangle(textbox: DocNode, features, scores, None)    =>
				{
				//val details = "\"" + features.mkString("<br/>") + "<hr/>" + scores.mkString("<br/>" + "\"")
				// val details = features.mkString("<br/>") + "<hr/>" + scores.mkString("<br/>")
				def bindPair(ss: Iterator[(String, Double)])(sTemplate: NodeSeq): NodeSeq =
					{
					(ss.flatMap
					 {
					 pair => bind("pair", sTemplate, "a" -> pair._1, "b" -> ("%3.2f" format pair._2))
					 }).toSeq
					}
				//.filter(_._2>0)
				bind("features", segmentTemplate, "features" -> bindPair(features.asSeq.map(f => (f._1.toString, f._2)).iterator) _,
				     "scores" -> bindPair(scores.asSeq.iterator) _, FuncAttrBindParam("id", (ns: NodeSeq) => Text(textbox.id), "id"))
				}
			case _                                                                => NodeSeq.Empty
			}
			}

		private def bindTextBoxes(textBoxes: Seq[DocNode])(textboxTemplate: NodeSeq): NodeSeq =
			{
			textBoxes.flatMap
			{
			textbox =>
				{
				bind("textbox", textboxTemplate, FuncAttrBindParam("style", (ns: NodeSeq) => addCoords(textbox, ns), "style"),
				     FuncAttrBindParam("class", (ns: NodeSeq) => addId(textbox, ns), "class"))
				}
			}
			}

		private def bindDelimitingBoxes(rectBoxes: Seq[DelimitingBox])(rectboxTemplate: NodeSeq): NodeSeq =
			{
			rectBoxes.flatMap
			{
			rectbox =>
				{
				bind("rectbox", rectboxTemplate, FuncAttrBindParam("style", (ns: NodeSeq) => addCoords(rectbox, ns), "style"),
				     FuncAttrBindParam("class", (ns: NodeSeq) => addId(rectbox, ns), "class"))
				}
			}
			}

		private def bindWhitespaceBoxes(rectBoxes: Seq[WhitespaceBox])(rectboxTemplate: NodeSeq): NodeSeq =
			{
			rectBoxes.flatMap
			{
			rectbox =>
				{
				bind("rectbox", rectboxTemplate, FuncAttrBindParam("style", (ns: NodeSeq) => addCoords(rectbox, ns), "style"),
				     FuncAttrBindParam("class", (ns: NodeSeq) => addId(rectbox, ns), "class"))
				}
			}
			}

		// redundant code
		private def bindDiscardBoxes(rectBoxes: Seq[DocNode])(rectboxTemplate: NodeSeq): NodeSeq =
			{
			rectBoxes.flatMap
			{
			rectbox =>
				{
				bind("rectbox", rectboxTemplate, FuncAttrBindParam("style", (ns: NodeSeq) => addCoords(rectbox, ns), "style"),
				     FuncAttrBindParam("class", (ns: NodeSeq) => addId(rectbox, ns), "class"))
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
				 {
				 text => bind("error", errorlineTemplate, "text" -> text)
				 }).toSeq
				}

			errors match
			{
				case None    => NodeSeq.Empty
				case Some(x) => bind("errorbox", errorboxTemplate, "errors" -> bindErrorLine(x) _);
			}
			}

		private def bindSuccessBox(info: Option[Iterator[String]])(successboxTemplate: NodeSeq): NodeSeq =
			{
			def bindSuccessLine(success: Iterator[String])(successlineTemplate: NodeSeq): NodeSeq =
				{
				(success.flatMap
				 {
				 text => bind("success", successlineTemplate, "text" -> text)
				 }).toSeq
				}

			info match
			{
				case None    => NodeSeq.Empty
				case Some(x) => bind("successbox", successboxTemplate, "successes" -> bindSuccessLine(x) _);
			}
			}

		private def addId(r: DocNode, ns: NodeSeq): NodeSeq = Text(r.id.replaceAll(" ", "") + " ") ++ ns

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

    private def addCoordsLabels(r: DocNode, ns: NodeSeq): NodeSeq =
    {
      val rr: RectangleOnPage = r.rectangle.get
      Text("position: absolute; top: " +
        (rr.page.rectangle.height - rr.top ) +
        //(rr.page.rectangle.height - rr.top) +
        "px; left: " + rr.page.rectangle.width +
        "px; width: 150" +
        "px; height: 23" + "px; ") ++ ns
    }

		}



}
