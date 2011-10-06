package edu.umass.cs.iesl.pdf2meta.webapp.lib

import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers

class UtilSpec extends FlatSpec with ShouldMatchers
  {

  "A List" should "Count lengths of runs with identity" in
  {
  val l = List(1, 3, 3, 3, 4, 4, 4, 4, 2, 2, 6, 6, 6, 6, 6, 6, 2, 2, 3, 3, 3)
  val result = Util.contiguousRuns(l)((x: Int) => x)
  val correct: List[(Int, List[Int])] = List((1, List(1)), (3, List(3,3,3)), (4, List(4,4,4,4)), (2, List(2,2)), (6, List(6,6,6,6,6,6)), (2, List(2,2)), (3, List(3,3,3)))

  assert(result == correct)
  //result should equal correct
  }

  it should "Count lengths of runs with mapping" in
  {
  val l = List("a", "abc", "bce", "def", "abcd", "abcd", "abcd", "abcd", "ab", "bc", "abcdef", "abcdef", "abcdef",
               "abcdef", "abcdef", "abcdeg", "xy", "yz", "xyz", "yzx", "zxy")
  val result = Util.contiguousRuns(l)((x: String) => x.length())
  val correct: List[(Int, List[String])] = List((1, List("a")), (3, List("abc", "bce", "def")), (4, List("abcd", "abcd", "abcd", "abcd")), (2, List("ab", "bc")), (6, List("abcdef", "abcdef", "abcdef",
               "abcdef", "abcdef", "abcdeg")), (2, List("xy", "yz")), (3, List("xyz", "yzx", "zxy")))

  assert(result == correct)
  }
  }
