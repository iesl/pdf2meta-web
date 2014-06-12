package edu.umass.cs.iesl.pdf2meta.webapp.snippet

/*
 * Copyright 2007-2010 WorldWide Conferencing, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


import _root_.net.liftweb._
import http._
import SHtml._

import common._
import util._
import Helpers._

import xml.{NodeSeq, Group}
import edu.umass.cs.iesl.pdf2meta.webapp.cakesnippet.{filenameBox, filestreamBox}

class Uploader
  {

  private object theUpload extends RequestVar[Box[FileParamHolder]](Empty)

  // the request-local variable that hold the file parameter
  /**
   * Bind the appropriate XHTML to the form
   */
  def upload(xhtml: Group): NodeSeq =
    {

    if (S.get_? || theUpload.is.isEmpty)
      {
      bind("ul", chooseTemplate("choose", "get", xhtml), "file_upload" -> fileUpload(ul => theUpload(Full(ul))))
      }
    else
      {
      val box: Box[FileParamHolder] = theUpload.is
      val v = box.openOrThrowException("exception") // box.openTheBox; kzaporojets, commented

      if (v.mimeType != "application/pdf")
      {
        println ("The following is the mimeType: " + v.mimeType)
        S.error("Not a PDF file")
      }

      filestreamBox.set(Full(v.fileStream))
      filenameBox.set(Full(v.fileName))
      S.redirectTo("showpdf")
      }
    };
  }

//kzaporojets: redirects to metatagger
class Uploadertometatagger
{

  private object theUpload extends RequestVar[Box[FileParamHolder]](Empty)

  // the request-local variable that hold the file parameter
  /**
   * Bind the appropriate XHTML to the form
   */
  def upload(xhtml: Group): NodeSeq =
  {
    if (S.get_? || theUpload.is.isEmpty)
    {
      bind("ul", chooseTemplate("choose", "get", xhtml), "file_upload" -> fileUpload(ul => theUpload(Full(ul))))
    }
    else
    {
      val box: Box[FileParamHolder] = theUpload.is
      val v = box.openOrThrowException("exception")

      if (v.mimeType != "application/pdf")
      {
        println ("The following is the mimeType: " + v.mimeType)
        S.error("Not a PDF file")
      }

      filestreamBox.set(Full(v.fileStream))
      filenameBox.set(Full(v.fileName))
      S.redirectTo("showmetatagger")
    }
  };
}
