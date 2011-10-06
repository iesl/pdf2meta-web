package edu.umass.cs.iesl.pdf2meta.webapp.snippet

import xml.NodeSeq
import net.liftweb.http.S
import net.liftweb.common.Full
import net.liftweb.util.BindHelpers._
import tools.nsc.io.{File, Directory}

/*
 * Created by IntelliJ IDEA.
 * User: lorax
 * Date: 9/9/11
 * Time: 4:49 PM
 */
class PdfExamples
  {
  def render(in: NodeSeq): NodeSeq =
    {
    val exampleDir = Directory("/Users/lorax/iesl/pdf2meta/ReadingOrderCases")
    val examples = exampleDir.files.toSeq.filter(x => !x.name.startsWith("."))


    def bindExamples(template: NodeSeq): NodeSeq =
      {
      examples.flatMap
        {
        example =>
          {
          bind("ex", template, "url" ->
                               {
                               val ename = example.name
                               S.fmapFunc(() => showExample(example))
                               {linkName =>
                                 {
                                 val linkUrl = "test.html?" + linkName + "=_"
                                 <a href={linkUrl}>
                                   {ename}
                                 </a>
                                 }
                               }
                               })
          }
        }
      }

    bind("examples", in, "count" -> examples.length, "example" -> bindExamples _)
    }

  def showExample(v: File): NodeSeq =
    {
    filestreamBox.set(Full(v.inputStream()))
    filenameBox.set(Full(v.name))
    S.redirectTo("showpdf")
    }
  }
