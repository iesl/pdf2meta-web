package edu.umass.cs.iesl.pdf2meta.webapp.cakesnippet

import xml.{Text, NodeSeq}
import net.liftweb.util.BindHelpers._
import net.liftweb.common.{Full, Box}
import net.liftweb.http.js.JsCmds._
import net.liftweb.http.js.JsCmd
import scala.Predef._
import edu.umass.cs.iesl.pdf2meta.webapp.lib.{PsToText, PdfToJpg}
import edu.umass.cs.iesl.pdf2meta.cli.coarsesegmenter._
import edu.umass.cs.iesl.pdf2meta.cli.WebPipelineComponent
import collection.Seq
import java.util.Date
import edu.umass.cs.iesl.scalacommons.StreamWorkspace
import edu.umass.cs.iesl.pdf2meta.cli.extract.metatagger.MetataggerBoxTextAtom
import scala.util.matching.Regex
import net.liftweb.http.S
import java.awt.font._
import org.specs2.internal.scalaz.std.int
import java.awt.Font
import java.awt.geom.AffineTransform

//import org.scala_tools.subcut.inject.AutoInjectable
import com.escalatesoft.subcut.inject.{Injectable, BindingModule}

import edu.umass.cs.iesl.pdf2meta.cli.layoutmodel._
import java.io.FileInputStream

//kzaporojets: modified for metatagger
trait ShowMetataggerComponent
	{
	this: WebPipelineComponent =>

	class ShowMetatagger(implicit val bindingModule:BindingModule) extends (NodeSeq => NodeSeq) with Injectable
		{
		def apply(in: NodeSeq): NodeSeq =
			{

//      val w = new StreamWorkspace(filenameBox.get.openOrThrowException("exception") , filestreamBox.get.openOrThrowException("exception"))
//
//      val psToText: PsToText = new PsToText(w)
//      val w_xml = new StreamWorkspace(psToText.outFilenameRunCrf , new FileInputStream(psToText.outFileRunCrf))
      val w = new StreamWorkspace("output_pstotext_runcrf_v2.pdf", new FileInputStream("/Users/klimzaporojets/klim/pdf2meta/pdf2meta-web/examples/output_pstotext_runcrf_v3.pdf"))

      val w_xml = new StreamWorkspace("output_pstotext_runcrf_v3.xml", new FileInputStream("/Users/klimzaporojets/klim/pdf2meta/pdf2meta-web/examples/output_pstotext_runcrf_v3.xml"))




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
        val (doc: DocNode, classifiedRectangles: ClassifiedRectangles) = metataggerPipeline.apply(w_xml)


				logger.debug("Conversion pipeline done ")
				val pipelineTime = new Date()
				logger.debug("Conversion pipeline took " + ((pipelineTime.getTime - extractTime.getTime)) + " milliseconds")
//
				val allDelimitingBoxes: Seq[DelimitingBox] = doc.delimitingBoxes
				val allWhitespaceBoxes: Seq[WhitespaceBox] = doc.whitespaceBoxes

				val allTextBoxes: Seq[DocNode] = doc.children //textBoxChildren
          println("the following value was saved in session: " + S.get("varv"))
				def bindPage(pageTemplate: NodeSeq): NodeSeq =
					{

					val leafRects: Seq[RectangleOnPage] = doc.leaves.map(_.rectangle).flatten
					val pages = leafRects.map(_.page).distinct.sortBy(_.pagenum)

					val boundPage = pages.flatMap
					                {
					                page =>
						                {
						                val rectangles: ClassifiedRectangles = classifiedRectangles.onPage(page)
						                val all: Seq[ClassifiedRectangle] = rectangles.raw


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
/*
    private def bindExternalLabels(sidelabels: Seq[ClassifiedRectangle], groupSideLabels:Seq[ClassifiedRectangle],
                                  divId:String, referencePattern:Regex, extSegmentTemplate: NodeSeq)(segmentTemplate: NodeSeq): NodeSeq =
* */
                          val reg = new scala.util.matching.Regex("""REFERENCE_([\d]+)_([\d]+)_([\d]+)_([\d]+)_([\d]+).*""", "coord1", "coord2", "coord3", "coord4", "pagenum")
                          val reg2 = new scala.util.matching.Regex("""REFERENCE_([\d]+)_([\d]+)_([\d]+)_([\d]+)_([\d]+).*reference$""", "coord1", "coord2", "coord3", "coord4", "pagenum")
 						                bind("page", pageTemplate,
						                     AttrBindParam("id", page.pagenum.toString, "id"),
						                     "image" -> image,
                                 "externallabels" -> bindExternalLabels(all, List(), "", "visible",
                                   reg
                                 ) _,
//                                 "sidelabels" -> bindSidelabels(all) _,
//						                     "segments" -> bindSegment(all) _,
//						                     "features" -> bindFeatures(all) _,
						                     "textboxes" -> bindTextBoxes(textBoxes, reg2) _,
                                 "pagenumber" ->  Text("---- Page " + page.pagenum + " ----")
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

// uncomment to clean the temporary directory
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

    private def getDistinctLabels(sidelabels:Seq[ClassifiedRectangle], labelsToIgnore:Seq[String]):Seq[ClassifiedRectangle] = {
      if(sidelabels.size>1)
      {
        //sidelabels.tail.exists(x => x.node.id == sidelabels.head.node.id)
        val recValue = getDistinctLabels(sidelabels.tail,labelsToIgnore)
        val headL= sidelabels.head
        if(recValue.exists(x => x.node.id == headL.node.id))
        {
          val value = recValue.find(x => x.node.id == headL.node.id)

          if(value.get.node.rectangle.get.top < headL.node.rectangle.get.top )
          {
            val (left, right) = recValue.span(_.node.id != headL.node.id)
            (headL +: left) ++ right.drop(1)
          }
          else
          {
            recValue
          }
        }
        else if (labelsToIgnore.exists(x=> x==headL.node.text))
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

    //returns the maximum top value
    private def maxValue(xs: Seq[ClassifiedRectangle]):Float = {
      if (xs.isEmpty) 0
      else {
        if( xs.head.node.rectangle.get.top >= maxValue(xs.tail) ) xs.head.node.rectangle.get.top
        else maxValue(xs.tail)
      }
    }
    private def organizeLabels(sidelabels: Seq[ClassifiedRectangle]):Seq[ClassifiedRectangle] = {
      def compfn1 (classRectangle1:ClassifiedRectangle, classRectangle2:ClassifiedRectangle) = (if (classRectangle1.node.rectangle.get.top.toInt !=classRectangle2.node.rectangle.get.top.toInt){classRectangle1.node.rectangle.get.top > classRectangle2.node.rectangle.get.top}
                                                                                            else {classRectangle1.node.text.length < classRectangle2.node.text.length})
//      def compfn1 (classRectangle1:ClassifiedRectangle, classRectangle2:ClassifiedRectangle) = (maxValue(sidelabels.filter(sl => sl.node.text == classRectangle1.node.text)) > maxValue(sidelabels.filter(sl => sl.node.text == classRectangle2.node.text)))
      val sortedLabels:Seq[ClassifiedRectangle] = sidelabels.sortWith(compfn1)
      //sortedLabels
      //50 because of margin
//      distributeLabels(sortedLabels, sortedLabels(0).node.rectangle.get.page.rectangle.height - 150)
      distributeLabels(sortedLabels, sortedLabels(0).node.rectangle.get.page.rectangle.height)
    }

//    private def distributeLabels(sortedSideLabels: Seq[ClassifiedRectangle], yCoord:Float):Seq[ClassifiedRectangle] = {
//      val headLabel:ClassifiedRectangle = sortedSideLabels.head
//
//      val repositionedHeadLabel:ClassifiedRectangle = headLabel.copy(node = new MetataggerBoxTextAtom(headLabel.node.id, headLabel.node.text.toUpperCase, "Font", 0.0f,
//        new RectangleOnPage {override val page: Page = headLabel.node.rectangle.get.page
//          override val bottom: Float = yCoord + 23
//          override val top: Float = yCoord
//          override val left: Float = headLabel.node.rectangle.get.left
//          override val right: Float = headLabel.node.rectangle.get.right
//        }, Array[Float](0f)))
//
//      if(sortedSideLabels.size>1)
//      {
//        val distributedL:Seq[ClassifiedRectangle] = distributeLabels(sortedSideLabels.tail, yCoord-25)
//        repositionedHeadLabel+:distributedL
//      }
//      else
//      {
//        List(repositionedHeadLabel)
//      }
//      //distributedL
//    }
    private def distributeLabels(sortedSideLabels: Seq[ClassifiedRectangle], yCoord:Float):Seq[ClassifiedRectangle] = {
      val headLabel:ClassifiedRectangle = sortedSideLabels.head

      if(sortedSideLabels.size>1)
      {
        if(yCoord>headLabel.node.rectangle.get.top)
        {
          val distributedL:Seq[ClassifiedRectangle] = distributeLabels(sortedSideLabels.tail, headLabel.node.rectangle.get.top-25)
          headLabel.copy(node = new MetataggerBoxTextAtom(headLabel.node.id, headLabel.node.text /*.toUpperCase*/, "Font", 0.0f,
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
          headLabel.copy(node = new MetataggerBoxTextAtom(headLabel.node.id, headLabel.node.text /*.toUpperCase*/, "Font", 0.0f,
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
          List(headLabel.copy(node = new MetataggerBoxTextAtom(headLabel.node.id, headLabel.node.text /*.toUpperCase*/, "Font", 0.0f,
            new RectangleOnPage {override val page: Page = headLabel.node.rectangle.get.page
              override val bottom: Float = headLabel.node.rectangle.get.top + 23
              override val top: Float = headLabel.node.rectangle.get.top
              override val left: Float = headLabel.node.rectangle.get.left
              override val right: Float = headLabel.node.rectangle.get.right
            }, Array[Float](0f))))
        }
        else
        {
          List(headLabel.copy(node = new MetataggerBoxTextAtom(headLabel.node.id, headLabel.node.text /*.toUpperCase*/, "Font", 0.0f,
            new RectangleOnPage {override val page: Page = headLabel.node.rectangle.get.page
              override val bottom: Float = yCoord + 23
              override val top: Float = yCoord
              override val left: Float = headLabel.node.rectangle.get.left
              override val right: Float = headLabel.node.rectangle.get.right
            }, Array[Float](0f))))
        }

      }
    }

    private def bindExternalLabels(sidelabels: Seq[ClassifiedRectangle], groupSideLabels:Seq[ClassifiedRectangle],
                                  divId:String, visibility:String, referencePattern:Regex)(segmentTemplate: NodeSeq): NodeSeq =
    {

        val headSidelabels = sidelabels.head
        val id:String = headSidelabels.node.id
  //      val referencePattern = new scala.util.matching.Regex("""REFERENCE_([\d]+)_([\d]+)_([\d]+)_([\d]+)_([\d]+)""", "coord1", "coord2", "coord3", "coord4", "pagenum")
       // def getDivId(divId:String, classRect:ClassifiedRectangle) = {if(divId==""){classRect.node.id}else{divId}}
        val m4 = referencePattern.findAllIn(id)
        if(!m4.isEmpty)
        {
          val coordRef = "REFERENCE_" + (m4 group "coord1") + "_" + (m4 group "coord2") + "_" + (m4 group "coord3") + "_" + (m4 group "coord4") + "_" + (m4 group "pagenum")

          if(coordRef == divId || groupSideLabels.size==0)
          {
            {if(sidelabels.size>=2) {bindExternalLabels(sidelabels.tail, headSidelabels +: groupSideLabels, coordRef, "hidden", referencePattern)(segmentTemplate)}
              else { bind("externallabel", segmentTemplate,
              FuncAttrBindParam("style", (ns: NodeSeq) => Text("visibility:hidden"), "style"),
              FuncAttrBindParam("id", (ns: NodeSeq) => Text(coordRef), "id"),
              "sidelabels" -> bindSidelabels(groupSideLabels) _ )}}
          }
          else
          {
              bind("externallabel", segmentTemplate,
                FuncAttrBindParam("style", (ns: NodeSeq) => Text("visibility:hidden"), "style"),
                FuncAttrBindParam("id", (ns: NodeSeq) => Text(divId), "id"),
                "sidelabels" -> bindSidelabels(groupSideLabels) _ ) ++
                {if(sidelabels.size>=2) {bindExternalLabels(sidelabels.tail, List(headSidelabels), coordRef, "hidden", referencePattern)(segmentTemplate)}
                      else { List()}}
          }
        }
        else
        {

          if(divId.contains("REFERENCE")) {
            bind("externallabel", segmentTemplate,
              FuncAttrBindParam("style", (ns: NodeSeq) => Text("visibility:" + visibility), "style"),
              FuncAttrBindParam("id", (ns: NodeSeq) => Text(divId), "id"),
              "sidelabels" -> bindSidelabels(groupSideLabels) _
            ) ++ {
              if (sidelabels.size >= 2) {
                bindExternalLabels(sidelabels.tail, List(headSidelabels), id, "visible", referencePattern)(segmentTemplate)
              }
              else {
                List()
              }
            }
          }
          else
          {
            {if(sidelabels.size>=2) {bindExternalLabels(sidelabels.tail, headSidelabels +: groupSideLabels, id, "visible", referencePattern)(segmentTemplate)}
            else {
              bind("externallabel", segmentTemplate,
              FuncAttrBindParam("style", (ns: NodeSeq) => Text("visibility:" + visibility), "style"),
              FuncAttrBindParam("id", (ns: NodeSeq) => Text(divId), "id"),
              "sidelabels" -> bindSidelabels(groupSideLabels ++ List(headSidelabels)) _
            )}}

          }

        }
    }
    private def bindSidelabels(sidelabels: Seq[ClassifiedRectangle])(segmentTemplate: NodeSeq): NodeSeq =
    {
      organizeLabels(getDistinctLabels(sidelabels, List("REFERENCES"))).flatMap
      {
        case x: ClassifiedRectangle =>
        {
          val truncatedText: String =
          {
            val t: String = x.node.text.trim
            val s = t.substring(0, t.length.min(300))
            s.length match
            {
              case 300 => s + " ..."
              case 0   => "EMPTY"
              case _   => s
            }
          }
          //calculates the width of truncated text

          val affinetransform:AffineTransform = new AffineTransform();
          val frc:FontRenderContext = new FontRenderContext(affinetransform,true,true);
          val font:Font = new Font("Helvetica Neue", Font.PLAIN, 12);
          val textwidth:Int = (font.getStringBounds(truncatedText, frc).getWidth()).toInt;
          val textheight:Int = (font.getStringBounds(truncatedText, frc).getHeight()).toInt;

          println ("textwidth for truncted text(" + truncatedText + "): " + textwidth)
          println ("textheight for truncted text(" + truncatedText + "): " + textheight)

//          if(truncatedText!="REFERENCES") {
            bind("sidelabel", segmentTemplate, "text" ->
              truncatedText,
              FuncAttrBindParam("class", (ns: NodeSeq) => (addId(x.node, ns) ++ Text((if (x.discarded) " discard" else ""))),
                "class"), FuncAttrBindParam("style", (ns: NodeSeq) => (addCoordsLabels(x.node, ns, textwidth)), "style"))
//          }
//          else
//          {
//            NodeSeq.Empty
//          }
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

		private def bindTextBoxes(textBoxes: Seq[DocNode], referencePattern:Regex)(textboxTemplate: NodeSeq): NodeSeq =
			{

			textBoxes.flatMap
			{
			textbox =>
				{
          val m4 = referencePattern.findAllIn(textbox.id)
          if(!m4.isEmpty)
          {
            println("bound textbox to: " + textbox.id)
            bind("textbox", textboxTemplate, FuncAttrBindParam("style", (ns: NodeSeq) => addCoords(textbox, ns), "style"),
              FuncAttrBindParam("class", (ns: NodeSeq) => Text("REFERENCE_" + (m4 group "coord1") + "_" + (m4 group "coord2") + "_" + (m4 group "coord3") + "_" + (m4 group "coord4") + "_" + (m4 group "pagenum"))
              /*(ns: NodeSeq) => addId(textbox, ns)*/, "class"))
          }
          else
          {
            //println("binding textbox: " + textbox.id)
            bind("textbox", textboxTemplate, FuncAttrBindParam("style", (ns: NodeSeq) => addCoords(textbox, ns), "style"),
               FuncAttrBindParam("class", (ns: NodeSeq) => addId(textbox, ns), "class"))
          }
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

    private def addCoordsLabels(r: DocNode, ns: NodeSeq, textWidth: Int): NodeSeq =
    {
      val rr: RectangleOnPage = r.rectangle.get
      Text("position: absolute; top: " +
        (rr.page.rectangle.height - rr.top ) +
        //(rr.page.rectangle.height - rr.top) +
        "px; left: " + rr.page.rectangle.width +
        "px; width: " + (textWidth + 10) +
        "px; height: 20" + "px; ") ++ ns
    }

		}



}
