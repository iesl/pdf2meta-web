package edu.umass.cs.iesl.pdf2meta.webapp.cakesnippet

import net.liftweb.http.SessionVar
import java.io.InputStream
import net.liftweb.common.{Empty, Box}
import edu.umass.cs.iesl.pdf2meta.webapp.lib.PageImage
import edu.umass.cs.iesl.pdf2meta.cli.layoutmodel.DocNode


object filestreamBox extends SessionVar[Box[InputStream]](Empty);

object filenameBox extends SessionVar[Box[String]](Empty);

object pageimages extends SessionVar[Map[Int, PageImage]](null);

object ReadingOrderPair
  {
  def joinPairs(list: List[DocNode]): List[ReadingOrderPair] =
    {
    // require list is sorted and nonoverlapping
    list match
    {
      case Nil => Nil
      case a :: t =>
        val ar = a.rectangle.get
        t match
        {
          case Nil => List(new ReadingOrderPair(ar.horizontalMiddle, ar.top, ar.horizontalMiddle, ar.bottom))
          case _ => List(new ReadingOrderPair(ar.horizontalMiddle, ar.top, ar.horizontalMiddle, ar.bottom), new
                          ReadingOrderPair(ar.horizontalMiddle, ar.bottom, t.head.rectangle.get.horizontalMiddle, t.head.rectangle.get.top)) :::
                    joinPairs(t)
        }
      // assert(b._1 > a._2)
    }
    }
  }

case class ReadingOrderPair(x1: Double, y1r: Double, x2: Double, y2r: Double)
  {
  val y1 = if (y2r - y1r > 10)
             {y1r - 3}
           else y1r
  val y2 = if (y2r - y1r > 10)
             {y2r - 3}
           else y2r
  }
