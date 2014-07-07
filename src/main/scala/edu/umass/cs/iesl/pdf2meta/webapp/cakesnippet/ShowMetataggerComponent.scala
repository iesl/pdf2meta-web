package edu.umass.cs.iesl.pdf2meta.webapp.cakesnippet

import scala.xml.{Node, Text, NodeSeq}
import net.liftweb.util.BindHelpers._
import net.liftweb.common.{Full, Box}
import net.liftweb.http.js.JsCmds._
import net.liftweb.http.js.JsCmd
import scala.Predef._
import edu.umass.cs.iesl.pdf2meta.webapp.lib.{Pdf2MetaWorkspace, MapToProperties, PsToText, PdfToJpg}
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
import scala.tools.nsc.io._
import scala.Some
import edu.umass.cs.iesl.pdf2meta.cli.coarsesegmenter.ClassifiedRectangle
import edu.umass.cs.iesl.pdf2meta.cli.layoutmodel.Page
import net.liftweb.common.Full
import scala.util.Random
import scala.reflect.io.File

import scala.xml.Utility._

//import org.scala_tools.subcut.inject.AutoInjectable
import com.escalatesoft.subcut.inject.{Injectable, BindingModule}

import edu.umass.cs.iesl.pdf2meta.cli.layoutmodel._
import java.io.FileInputStream
import scala.xml._

//kzaporojets: modified for metatagger
trait ShowMetataggerComponent
	{


	this: WebPipelineComponent =>


  val allowDuplicates:Seq[String] = List("CONTENT -> HEADERS -> AUTHORS -> AUTHOR -> AUTHOR-FIRST",
                                          "CONTENT -> HEADERS -> AUTHORS -> AUTHOR -> AUTHOR-LAST",
                                          "CONTENT -> BIBLIO -> REFERENCE -> AUTHORS -> AUTHOR -> AUTHOR-FIRST",
                                          "CONTENT -> BIBLIO -> REFERENCE -> AUTHORS -> AUTHOR -> AUTHOR-LAST",
                                          "CONTENT -> BIBLIO -> REFERENCE -> AUTHORS -> AUTHOR -> AUTHOR-MIDDLE")

  //links the path of the children to the respective prefix of each one (AUTHOR x, INSTITUTION x, etc...)
  val mapPrefixChildren:Map[String, String] = Map("HEADERS -> AUTHORS" -> "AUTHOR",
        "HEADERS -> INSTITUTION" -> "INSTITUTION",
        "HEADERS -> ADDRESS" -> "ADDRESS",
        "HEADERS -> NOTE -> DATE" -> "DATE",
        "HEADERS -> NOTE -> INSTITUTION" -> "INSTITUTION",
        "HEADERS -> NOTE -> ADDRESS" -> "ADDRESS",
        "HEADERS -> EMAIL" -> "EMAIL")

  //list that contains the children whose textbox have to be bound
  val childrenToBind:Seq[String] = List("HEADERS -> INSTITUTION",
                                        "HEADERS -> ADDRESS",
                                        "HEADERS -> NOTE -> DATE",
                                        "HEADERS -> NOTE -> INSTITUTION",
                                        "HEADERS -> NOTE -> ADDRESS",
                                        "HEADERS -> EMAIL"
                                        //
                                        )

  /*
  *
    val mapAcceptedLabels:Map[String, String] = Map("CONTENT -> HEADERS -> TITLE" -> "HEADERS -> TITLE",
    "CONTENT -> HEADERS -> AUTHORS" -> "HEADERS -> AUTHORS",
    "CONTENT -> HEADERS -> INSTITUTION" -> "HEADERS -> INSTITUTION",
  * */

	class ShowMetatagger(implicit val bindingModule:BindingModule) extends (NodeSeq => NodeSeq) with Injectable
		{
		def apply(in: NodeSeq): NodeSeq =
			{


      //val w = new StreamWorkspace(filenameBox.get.openOrThrowException("exception") , filestreamBox.get.openOrThrowException("exception"))

      val wNewStructure = new Pdf2MetaWorkspace(filenameBox.get.openOrThrowException("exception") , filestreamBox.get.openOrThrowException("exception"))



      val psToText: PsToText = new PsToText(wNewStructure/*w*/)
      val w_xml = new StreamWorkspace(psToText.outFilenameRunCrf , new FileInputStream(psToText.outFileRunCrf))

//      val w = new StreamWorkspace("output_pstotext_runcrf_v2.pdf", new FileInputStream("/Users/klimzaporojets/klim/pdf2meta/pdf2meta-web/examples/output_pstotext_runcrf_v3.pdf"))
//
//      val w_xml = new StreamWorkspace("output_pstotext_runcrf_v3.xml", new FileInputStream("/Users/klimzaporojets/klim/pdf2meta/pdf2meta-web/examples/output_pstotext_runcrf_v3.xml"))


      val length: Box[Text] = Full(Text(wNewStructure.file.length.toString /*w.file.length.toString*/))

			val filename: Box[Text] = Full(Text(wNewStructure.file.name.toString /*w.filename*/))

			def bindPdfInfo(template: NodeSeq): NodeSeq =
				{

				logger.debug("Starting PDF image generation...")
				val startTime = new Date
				logger.debug("Running PDF image generation...")

				//** could use pdfbox converter here
				val pdfToJpg: PdfToJpg = new PdfToJpg(wNewStructure /*w*/) // stores images in session as side effect
        val props:MapToProperties = new MapToProperties()
          val propertiesFilename:String =
            S.get("propertiesFile").openOrThrowException("err while obtaining properties file")

          val properties:Map[String,String] = props.readPropertiesFile(propertiesFilename)

        //mapToP.addToProperties()
				logger.debug("PDF image generation done ")
				val extractTime = new Date()
				logger.debug("PDF image generation took " + ((extractTime.getTime - startTime.getTime)) + " milliseconds")
        val (dokiss: DocNode, classifiedRectangles: ClassifiedRectangles) = metataggerPipeline.apply(w_xml)


				logger.debug("Conversion pipeline done ")
				val pipelineTime = new Date()
				logger.debug("Conversion pipeline took " + ((pipelineTime.getTime - extractTime.getTime)) + " milliseconds")

        S.set("state","uploading")
        S.set("message","90%: parsing xml generated by metatagger")
        S.set("percentage","90")

				def bindPage(pageTemplate: NodeSeq): NodeSeq =
					{

					val leafRects: Seq[RectangleOnPage] = classifiedRectangles.raw.map(_.node.rectangle.get) //doc.leaves.map(_.rectangle).flatten
					val pages = leafRects.map(_.page).distinct.sortBy(_.pagenum)

					val boundPage = pages.flatMap
					                {
					                page =>
						                {
						                val rectangles: ClassifiedRectangles = classifiedRectangles.onPage(page)
						                val all: Seq[ClassifiedRectangle] = rectangles.raw


						                val image = pageimages.get(page.pagenum).imageUrl

                            val reg = new scala.util.matching.Regex("""REFERENCE_([\d]+)_([\d]+)_([\d]+)_([\d]+)_([\d]+).*""", "coord1", "coord2", "coord3", "coord4", "pagenum")
                            val reg2 = new scala.util.matching.Regex("""REFERENCE_([\d]+)_([\d]+)_([\d]+)_([\d]+)_([\d]+).*reference$""", "coord1", "coord2", "coord3", "coord4", "pagenum")
 						                bind("page", pageTemplate,
						                     AttrBindParam("id", page.pagenum.toString, "id"),
                                FuncAttrBindParam("style", (ns: NodeSeq) => addPlainCoords("0","0",
                                  properties.get("width").get /*pdfToJpg.width*/,
                                  properties.get("height").get /*pdfToJpg.height*/,ns), "style"),
						                     "image" -> image,
                                 "externallabels" -> bindExternalLabels(all, List(), "", "visible",
                                   reg
                                 ) _,
                                  "textboxes" -> bindTextBoxesV2(all,reg2) _,
                                 "pagenumber" ->  Text("---- Page " + page.pagenum + " ----")
                            )
						                };
					                }
					boundPage
					}


				val result = bind("pdfinfo", template, //"filename" -> Text(wNewStructure.file.toString() /*w.file.toString()*/ ),
				               "pages" -> bindPage _)

// uncomment to clean the temporary directory
//				logger.debug("Clearing temporary directory " + w.dir)
//				w.clean()

				logger.debug("HTML Rendering done ")
				val renderTime = new Date()
				logger.debug("HTML Rendering took " + ((renderTime.getTime - pipelineTime.getTime)) + " milliseconds")
				result
				}

      val xmlmetatagger = pagexmls.get("metatagger").xmlUrl

			val res = bind("ul", in, "file_name" -> filename,
			     // "mime_type" -> mimetype, // Text(v.mimeType)),
			     "length" -> length, //"md5" -> md5str,
			     "pdfinfo" -> bindPdfInfo _,
           "xmlmetatagger" -> xmlmetatagger)


        S.set("state","uploading")
        S.set("message","99%: binding the content")
        S.set("percentage","99")

        res

			}

		private def bindSegment(segments: Seq[ClassifiedRectangle])(segmentTemplate: NodeSeq): NodeSeq =
			{
			segments.flatMap
			{
			case x: ClassifiedRectangle =>
				{

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





    private def getDistinctLabels(sidelabels:Seq[ClassifiedRectangle], labelsToIgnore:Seq[String]):Seq[ClassifiedRectangle] = {
      if(sidelabels.size>1)
      {
        val recValue = getDistinctLabels(sidelabels.tail,labelsToIgnore)
        val headL= sidelabels.head
        if(recValue.exists(x => x.node.id == headL.node.id)/* && !(allowDuplicates.exists(x=> headL.node.id.toUpperCase().contains(x)))*/)
        {
          val value = recValue.find(x => x.node.id == headL.node.id)

          if(value.get.node.rectangle.get.top < headL.node.rectangle.get.top )
          {
            val (left, right) = recValue.span(_.node.id != headL.node.id)


            val headLWChildren = headL.copy(children = headL.children ++ value.get.children )
            (headLWChildren +: left) ++ right.drop(1)
          }
          else
          {
            val (left, right) = recValue.span(_.node.id != headL.node.id)
            val headLWChildren = value.get.copy(children = headL.children ++ value.get.children )
            (left :+ headLWChildren) ++ right.drop(1)
//            recValue
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

      val sortedLabels:Seq[ClassifiedRectangle] = sidelabels.sortWith(compfn1)
      distributeLabels(sortedLabels, sortedLabels(0).node.rectangle.get.page.rectangle.height)
    }

    private def distributeLabels(sortedSideLabels: Seq[ClassifiedRectangle], yCoord:Float):Seq[ClassifiedRectangle] = {

      val affinetransform:AffineTransform = new AffineTransform();
      val frc:FontRenderContext = new FontRenderContext(affinetransform,true,true);
      val font:Font = new Font("Helvetica Neue", Font.PLAIN, 12);


      val headLabel:ClassifiedRectangle = sortedSideLabels.head


      //the function that distributes in lines and calculates the max width
      def breakText(leftTokens:List[String], accumulatedTokens:List[String], widthSoFar:Int, /* largestWidth:Int, */ maxWidth:Int):(String, Int)=
      {
        if(leftTokens.size>0) {
          val firstElement = leftTokens.head
          if (widthSoFar + font.getStringBounds({if((firstElement + " ").trim==""){""}else{firstElement + " "}}, frc).getWidth().toInt > maxWidth)
          {
            val retVal = breakText(leftTokens,List(),0, /*{if(widthSoFar > largestWidth){widthSoFar}else
                  {largestWidth}},*/ maxWidth)
            (Utility.escape(accumulatedTokens.map(x=> x.toString).mkString(" ")) + " \n<br></br> " + retVal._1,
              {if(widthSoFar > retVal._2){widthSoFar}else{retVal._2}})
          }
          else
          {
            breakText({if(leftTokens.size>1){leftTokens.tail}else{List()}},
               accumulatedTokens :+ firstElement, widthSoFar + font.getStringBounds({if((firstElement + " ").trim==""){""}else{firstElement + " "}}, frc).getWidth().toInt, maxWidth)
          }
        }
        else
        {
          (Utility.escape(accumulatedTokens.map(x=> x.toString).mkString(" ")),widthSoFar)
        }
      }

      def addTextChildren(rect:Seq[ClassifiedRectangle], number:Int, widthSoFar:Int, pathParent:String):(String, Int) =
      {
        if(rect.size>0)
        {
          val currentRect = rect.head
          val childName = mapPrefixChildren.get(pathParent)

          val (brokenLines:String, maxWidth:Int) = breakText((currentRect.node.text).split(" ").toList,
          List(), 0, 350)
          /*breakText(headL.node.text.split(" ").toList, List(),
                                                      0, /* largestWidth:Int, */ 400)*/


          val rectRes:String = "&#160;&#160;&#160;&#160;&#160;<strong>" +
            {if(!childName.isEmpty){childName.get}else{childName}} + " " + number + ":</strong> " + brokenLines.replaceAll("FN:", "<strong>FN:</strong>").replaceAll("LN:","<strong>LN:</strong>")
                      .replaceAll("MN:","<strong>MN:</strong>").replaceAll("<br></br>","<br></br>&#160;&#160;&#160;&#160;")
          //TODO:Utility.unescape(rectRes) may be needed to determine the width more accurately
          val textWidth:Int = font.getStringBounds("     " + {if(!childName.isEmpty){childName.get}else{childName}} +
                  " " + number + ":",
                      frc).getWidth().toInt + maxWidth



//          val rectRes:String = "&#160;&#160;&#160;&#160;&#160;<strong>" +
//            {if(!childName.isEmpty){childName.get}else{childName}} + " " + number + ":</strong> " + (Utility.escape(currentRect.node.text)).replaceAll("FN:", "<strong>FN:</strong>").replaceAll("LN:","<strong>LN:</strong>")
//                      .replaceAll("MN:","<strong>MN:</strong>")
//          //TODO:Utility.unescape(rectRes) may be needed to determine the width more accurately
//          val textWidth:Int = font.getStringBounds(rectRes.replaceAll("<strong>","").replaceAll("</strong>","").replaceAll("&#160;", " "), frc).getWidth().toInt

          if(rect.size>1)
          {
            val tchil = addTextChildren(rect.tail,number+1,{if(textWidth>widthSoFar){textWidth}else{widthSoFar}}, pathParent)
            ("\n<br></br>" + rectRes + tchil._1, {if(textWidth>widthSoFar && textWidth>tchil._2){textWidth}else if(widthSoFar>tchil._2){widthSoFar} else{tchil._2}})
          }
          else
          {
            ("\n<br></br>" + rectRes, {if(textWidth>widthSoFar){textWidth}else{widthSoFar}})
          }
        }
        else
        {
          ("",widthSoFar)
        }


      }
      //the function to copy the label, incorporating the newly generated text (with lines), and the vertical distances
      def copyHeadLabel(headL:ClassifiedRectangle, topRel:Float):ClassifiedRectangle =
      {

        //trims to 1000 chars
        val truncatedText: String =


        {
          val t: String = headL.node.text.trim
          //only for abstract
          if(headL.node.id.toUpperCase()=="CONTENT -> HEADERS -> ABSTRACT")
          {
            val s =

            t.substring(0, t.length.min(100))
            s.length match
            {

              case 100 => s + t.substring(100, t.indexOf(" ", 100)) +  " [...] " + {if(t.length<=100){""} else {t.substring( t.indexOf(" ", {if(t.length-100<100){100}else{t.length-100}}),t.length)}}
              case 0   => ""
              case _   => s
            }
          }
          else
          {
            t
          }
        }

        val ( tokenizedText:String,  maxWidth:Int) = breakText(truncatedText.split(" ").toList /*headL.node.text.split(" ").toList*/, List(),
                                                      0, 400)



        val (childrenText:String, maxChildrenWidth:Int) = addTextChildren(headL.children,1,maxWidth, headL.node.text)
        //the children text is added

        def boldenPart(tokenizedText:String):String = {
          if(tokenizedText.indexOf(":") > -1)
          {
            ("<strong>" + tokenizedText.substring(0,tokenizedText.indexOf(":")+1) + "</strong>" + tokenizedText.substring(tokenizedText.indexOf(":")+1,tokenizedText.length)).replace("[...]","<strong>[...]</strong>")
          }
          else
          {
            ("<strong>" + tokenizedText + "</strong>").replace("[...]","<strong>[...]</strong>")
          }
        }
        val finalTextWithChildren = boldenPart(tokenizedText) + childrenText

        headL.copy(node = new MetataggerBoxTextAtom(headL.node.id, finalTextWithChildren /*boldenPart(tokenizedText)*/ /*headL.node.text*/ /*.toUpperCase*/, "Font", 0.0f,
          new RectangleOnPage {override val page: Page = headL.node.rectangle.get.page
            override val bottom: Float = topRel + (18 * finalTextWithChildren.split("\n").length + 5 )
            override val top: Float = topRel
            override val left: Float = headL.node.rectangle.get.left
            override val right: Float = headL.node.rectangle.get.left + maxChildrenWidth // maxWidth //headL.node.rectangle.get.right
          }, Array[Float](0f)))
      }
      if(sortedSideLabels.size>1)
      {
        if(yCoord>headLabel.node.rectangle.get.top)
        {
          val copiedHeadLabel = copyHeadLabel(headLabel,headLabel.node.rectangle.get.top)
          val distributedL:Seq[ClassifiedRectangle] = distributeLabels(sortedSideLabels.tail, headLabel.node.rectangle.get.top-(copiedHeadLabel.node.rectangle.get.bottom -
                                                                                                                    copiedHeadLabel.node.rectangle.get.top))
          copiedHeadLabel+:distributedL
        }
        else
        {
          val copiedHeadLabel = copyHeadLabel(headLabel,yCoord)
          val distributedL:Seq[ClassifiedRectangle] = distributeLabels(sortedSideLabels.tail, yCoord-(copiedHeadLabel.node.rectangle.get.bottom -
            copiedHeadLabel.node.rectangle.get.top))
          copiedHeadLabel+:distributedL
        }
      }
      else
      {
        if(yCoord>headLabel.node.rectangle.get.top) {
          val copiedHeadLabel = copyHeadLabel(headLabel,headLabel.node.rectangle.get.top)
          List(copiedHeadLabel)
        }
        else
        {
          val copiedHeadLabel = copyHeadLabel(headLabel,yCoord)
          List(copiedHeadLabel)

        }

      }
    }

    private def bindExternalLabels(sidelabels: Seq[ClassifiedRectangle], groupSideLabels:Seq[ClassifiedRectangle],
                                  divId:String, visibility:String, referencePattern:Regex)(segmentTemplate: NodeSeq): NodeSeq =
    {

        val headSidelabels = sidelabels.head
        val id:String = headSidelabels.node.id
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
            val s = t.substring(0, t.length.min(100000))
            s.length match
            {
              case 100000 => s + " ..."
              case 0   => "EMPTY"
              case _   => s
            }
          }


          val testText:String = "<font>" + truncatedText + "</font>"

//          println("about to load: " + testText);
          val brokenText:NodeSeq =  XML.loadString(testText) // testTextList.map(x=> {<font>{x}<br></br></font>}) //{<font>just test</font>} //breakText(truncatedText.split(" ").toList, List(), 0, 0, 60)

          bind("sidelabel", segmentTemplate, "text" ->
              /*truncatedText*/brokenText,
              FuncAttrBindParam("class", (ns: NodeSeq) => (addId(x.node, ns) ++ Text((if (x.discarded) " discard" else ""))),
                "class"), FuncAttrBindParam("style", (ns: NodeSeq) => (addCoordsLabels(x.node, ns)), "style"))
        }
        case _                      => NodeSeq.Empty
      }
    }

		private def bindFeatures(segments: Seq[ClassifiedRectangle])(segmentTemplate: NodeSeq): NodeSeq =
			{
			segments.flatMap
			{
			case ClassifiedRectangle(textbox: DocNode, features, scores, Some(x), List()) =>
				{
				def bindPair(ss: Iterator[(String, Double)])(sTemplate: NodeSeq): NodeSeq =
					{
					(ss.flatMap
					 {
					 pair => bind("pair", sTemplate, "a" -> pair._1, "b" -> ("%3.2f" format pair._2))
					 }).toSeq
					}
				bind("features", segmentTemplate, "features" -> bindPair(x.featureWeights.asSeq.map(f => (f._1.toString, f._2)).iterator) _,
				     "scores" -> bindPair(x.labelWeights.asSeq.iterator) _, FuncAttrBindParam("id", (ns: NodeSeq) => Text(textbox.id), "id"))
				}
			case ClassifiedRectangle(textbox: DocNode, features, scores, None, List())    =>
				{
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
            bind("textbox", textboxTemplate, FuncAttrBindParam("style", (ns: NodeSeq) => addCoords(textbox, ns), "style"),
              FuncAttrBindParam("class", (ns: NodeSeq) => Text("REFERENCE_" + (m4 group "coord1") + "_" + (m4 group "coord2") + "_" + (m4 group "coord3") + "_" + (m4 group "coord4") + "_" + (m4 group "pagenum"))
              /*(ns: NodeSeq) => addId(textbox, ns)*/, "class"))
          }
          else
          {
            bind("textbox", textboxTemplate, FuncAttrBindParam("style", (ns: NodeSeq) => addCoords(textbox, ns), "style"),
               FuncAttrBindParam("class", (ns: NodeSeq) => addId(textbox, ns), "class"))
          }
				}
			}
			}


    private def bindTextBoxesV2(textBoxes: Seq[ClassifiedRectangle], referencePattern:Regex)(textboxTemplate: NodeSeq): NodeSeq =
    {

      textBoxes.flatMap
      {
        textbox =>
        {
          val m4 = referencePattern.findAllIn(textbox.node.id)
          if(!m4.isEmpty)
          {
            bind("textbox", textboxTemplate, FuncAttrBindParam("style", (ns: NodeSeq) => addCoords(textbox.node , ns), "style"),
              FuncAttrBindParam("class", (ns: NodeSeq) => Text("REFERENCE_" + (m4 group "coord1") + "_" + (m4 group "coord2") + "_" + (m4 group "coord3") + "_" + (m4 group "coord4") + "_" + (m4 group "pagenum"))
                /*(ns: NodeSeq) => addId(textbox, ns)*/, "class"))
          }
          else
          {
            bind("textbox", textboxTemplate, FuncAttrBindParam("style", (ns: NodeSeq) => addCoords(textbox.node, ns), "style"),
              FuncAttrBindParam("class", (ns: NodeSeq) => addId(textbox.node, ns), "class")) ++
           {if(childrenToBind.contains(textbox.node.text)) {
              textbox.children.flatMap {
                tbChildren => {
                  bind("textbox", textboxTemplate, FuncAttrBindParam("style", (ns: NodeSeq) => addCoords(tbChildren.node, ns), "style"),
                    FuncAttrBindParam("class", (ns: NodeSeq) => addId(textbox.node, ns), "class"))
                }
              }
            }
            else
            {
              List()
            }}
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

    private def addCoordsLabels(r: DocNode, ns: NodeSeq): NodeSeq =
    {
      val rr: RectangleOnPage = r.rectangle.get
      Text("position: absolute; top: " +
        (rr.page.rectangle.height - rr.top ) +
        //(rr.page.rectangle.height - rr.top) +
        "px; left: " + rr.page.rectangle.width +
        "px; width: " + ((r.rectangle.get.right - r.rectangle.get.left).toInt + 12) +
        "px; height: " + (r.rectangle.get.bottom - r.rectangle.get.top) +  "px; ") ++ ns
    }

    private def addPlainCoords(left:String, top:String, width:String, height:String, ns:NodeSeq): NodeSeq =
    {
      Text("position: relative; top: " + top +
        //(rr.page.rectangle.height - rr.top) +
        "px; left: " + left +
        "px; width: " + width +
        "px; height: " + height + "px; " ) ++ ns
    }

		}



}
