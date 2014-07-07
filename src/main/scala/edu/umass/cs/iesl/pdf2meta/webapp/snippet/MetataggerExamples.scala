package edu.umass.cs.iesl.pdf2meta.webapp.snippet

import xml.NodeSeq
import net.liftweb.http.S
import net.liftweb.common.Full
import net.liftweb.util.BindHelpers._
import tools.nsc.io.{File, Directory}
import edu.umass.cs.iesl.pdf2meta.webapp.cakesnippet.{filenameBox, filestreamBox}
import java.io

//import org.scala_tools.subcut.inject.AutoInjectable
import com.escalatesoft.subcut.inject.{Injectable, BindingModule}
//import org.scala_tools.subcut.inject.AutoInjectable

import scala.reflect.io.File
import edu.umass.cs.iesl.pdf2meta.webapp.lib.MapToProperties
/*
 * Created by IntelliJ IDEA.
 * User: lorax
 * Date: 9/9/11
 * Time: 4:49 PM
 */
class MetataggerExamples(implicit val bindingModule:BindingModule) extends Injectable
 {

  S.set("state","uploading")
  S.set("message","1%: initializing")
  S.set("percentage","1")

//  val exampleDirPath = inject[String]('examples)

  val propertiesDirPath = inject[String]('properties_path)

  val propertiesDir = Directory(propertiesDirPath)

//  val exampleDir = Directory(exampleDirPath)
  //val exampleDir = Directory(System.getProperty("pdf2metaExamples")) //"/Users/lorax/iesl/pdf2meta/ReadingOrderCases") //Directory("/Users/lorax/iesl/bibmogrify-project/pdf2meta-web/examplePDFs") //
  val mapToProperties = new MapToProperties

  def render(in: NodeSeq): NodeSeq = {
    // reread these on every render, to allow changing files while the server is runng
//    val examples = exampleDir.files.toSeq.filter(x => !x.name.startsWith(".") && x.name.contains(".pdf"))
//    exampleDir.files
    val examples:Seq[scala.reflect.io.File] = propertiesDir.files.toSeq
      .filter(x=>x.name.contains(".properties") && mapToProperties.readPropertiesFile(x.path /*+ x.name*/).contains("pdflocation"))
      .map(x=>
            new scala.reflect.io.File(
              new java.io.File(propertiesDirPath + java.io.File.separator +  mapToProperties.readPropertiesFile(x.path /* + x.name*/).get("pdflocation").get))
       )

//    println(examples)
    def bindExamples(template: NodeSeq): NodeSeq = {
      examples.flatMap {
        example => {
          bind("ex", template, "url" -> {
            val ename = example.name
            S.fmapFunc(() => showExample(example)) {
              linkName => {
                val linkUrl = "pdfexamplesmetatagger?" + linkName + "=_"
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
    S.redirectTo("showmetatagger")
  }
}
