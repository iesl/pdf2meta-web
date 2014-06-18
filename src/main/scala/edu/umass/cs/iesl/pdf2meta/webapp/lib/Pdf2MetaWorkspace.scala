package edu.umass.cs.iesl.pdf2meta.webapp.lib

import edu.umass.cs.iesl.scalacommons.{TempDirFactory, Workspace}
import java.io.InputStream
import scala.tools.nsc.io._
import scalax.io.Resource
import com.escalatesoft.subcut.inject.{BindingModule, Injectable}
import net.liftweb.http._

/**
 * Created by klimzaporojets on 6/17/14.
 * Generates all the file structure for a particular pdf file if it wasnt previously generated
 */
class Pdf2MetaWorkspace(val filename: String, instream: InputStream)(implicit val bindingModule: BindingModule) extends Workspace  with Injectable{


  val (dir, file) = {

    val d = TempDirFactory()
    val f = File(d + File.separator + filename)
    val outstream = f bufferedOutput (false)
    Resource.fromInputStream(instream) copyDataTo Resource.fromOutputStream(outstream)
    instream.close()
    outstream.close()

    def getOnlyFileName(fileName:String):String =
    {
      fileName.substring(0,fileName.lastIndexOf("."))
    }
    def getPropertyFileName(fileName:String):String =
    {
      getOnlyFileName(fileName) + ".properties"
    }
    val propertiesPath = inject[String]('properties_path)
    val mainPropertiesLocation = propertiesPath + getPropertyFileName(filename)

    S.set("propertiesFile",mainPropertiesLocation)

    //if the main properties file doesn't exist, it creates it assigning some of the properties
    val propertiesMapper:MapToProperties = new MapToProperties

    val properties:Map[String,String] = propertiesMapper.readPropertiesFile(mainPropertiesLocation)

    def getDirAndFile():(scala.reflect.io.Directory, scala.reflect.io.File)=
    {

      if((properties.get("pdflocation")!=None && properties.get("pdflocation").get.trim!="" &&
        linuxCommandExecuter.runCommand("diff " + properties.get("pdflocation").get +
          " " + d + File.separator + filename).trim!=""))
      {
        linuxCommandExecuter.runCommand("cp " + d + File.separator + filename + " " +
                properties.get("pdflocation").get)
        propertiesMapper.savePropertiesValues(mainPropertiesLocation,
                                      Map("pdflocation" -> properties.get("pdflocation").get,
                                          "ispdfalreadyparsed" -> "false" ))

      }
      else if(properties.get("pdflocation")==None ||
                    properties.get("pdflocation").get.trim=="")
      {
        val pdfDir = propertiesPath + File.separator + "data" + File.separator + getOnlyFileName(filename)

        File(pdfDir).createDirectory(force=true)

        linuxCommandExecuter.runCommand("cp " + d + File.separator + filename + " " +
          pdfDir + File.separator)
        propertiesMapper.savePropertiesValues(mainPropertiesLocation,
          Map("pdflocation" -> (pdfDir + File.separator + filename),
            "ispdfalreadyparsed" -> "false" ))
      }
      else
      {
        propertiesMapper.addOrReplaceValue(mainPropertiesLocation, "ispdfalreadyparsed", "true")
      }

      val modifiedProperties:Map[String,String] = propertiesMapper.readPropertiesFile(mainPropertiesLocation)
      (File(modifiedProperties.get("pdflocation").get.substring(0, modifiedProperties.get("pdflocation").get.lastIndexOf("/"))).createDirectory(),
            File(modifiedProperties.get("pdflocation").get))
    }

    val(dir,file) = getDirAndFile()
    //deletes the temp file
    d.deleteRecursively()

    (dir,file)
  }

  def clean() {
    dir deleteRecursively()
  }
}
