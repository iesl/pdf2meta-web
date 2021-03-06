package edu.umass.cs.iesl.pdf2meta.webapp.snippet

import xml.NodeSeq
import net.liftweb.http.S
import net.liftweb.common.Full
import net.liftweb.util.BindHelpers._
import tools.nsc.io.{File, Directory}
import edu.umass.cs.iesl.pdf2meta.webapp.cakesnippet.{filenameBox, filestreamBox}
//import org.scala_tools.subcut.inject.AutoInjectable
import com.escalatesoft.subcut.inject.{Injectable, BindingModule}
//import org.scala_tools.subcut.inject.AutoInjectable

/*
 * Created by IntelliJ IDEA.
 * User: lorax
 * Date: 9/9/11
 * Time: 4:49 PM
 */
class PdfExamples(implicit val bindingModule:BindingModule) extends Injectable
 {

  val exampleDirPath = inject[String]('examples)
//  println("The path that fails is: " + exampleDirPath)
  val exampleDir = Directory(exampleDirPath)
  //val exampleDir = Directory(System.getProperty("pdf2metaExamples")) //"/Users/lorax/iesl/pdf2meta/ReadingOrderCases") //Directory("/Users/lorax/iesl/bibmogrify-project/pdf2meta-web/examplePDFs") //


  def render(in: NodeSeq): NodeSeq = {
//    println("Hello world!")
//    NodeSeq.Empty
    // reread these on every render, to allow changing files while the server is runng
    val examples = exampleDir.files.toSeq.filter(x => !x.name.startsWith("."))

    def bindExamples(template: NodeSeq): NodeSeq = {
      examples.flatMap {
        example => {
          bind("ex", template, "url" -> {
            val ename = example.name
            S.fmapFunc(() => showExample(example)) {
              linkName => {
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

  def showExample(v: File): NodeSeq = {
    filestreamBox.set(Full(v.inputStream()))
    filenameBox.set(Full(v.name))
    S.redirectTo("showpdf")
  }
}
