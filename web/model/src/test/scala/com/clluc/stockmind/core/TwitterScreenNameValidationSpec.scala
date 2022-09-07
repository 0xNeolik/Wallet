package com.clluc.stockmind.core

import com.clluc.stockmind.core.twitter.TwitterHandle
import org.scalatest.FlatSpec

class TwitterScreenNameValidationSpec extends FlatSpec {

  private def assertHandler(name: String, valid: Boolean) =
    if (valid)
      assert(TwitterHandle(name).value == name)
    else
      assertThrows[IllegalArgumentException](TwitterHandle(name))

  // TODO do this with Scalacheck generators
  // it is going to take me some time to create a good generator for twitter handlers (I feel "as thick as a brick today")
  // as we are in a rush, I leave this for later

  "An screen name that comes with a leading @" should "be declared invalid" in {
    assertHandler("@marianongdev", false)
  }

  "An screen name just with lower case letters" should "be declared valid" in {
    assertHandler("marianongdev", true)
  }

  "An screen name just with upper case letters" should "be declared valid" in {
    assertHandler("MARIANONGDEV", true)
  }

  "An screen name just with numbers" should "be declared valid" in {
    assertHandler("1232423423", true)
  }

  "A one number screen name" should "be declared valid" in {
    assertHandler("3", true)
  }

  "A one lower case letter name" should "be declared valid" in {
    assertHandler("f", true)
  }

  "A one upper case letter name" should "be declared valid" in {
    assertHandler("G", true)
  }

  "A one underscore screen name" should "be declared valid" in {
    assertHandler("_", true)
  }

  "15 underscores as screen name" should "be declared valid" in {
    assertHandler("_______________", true)
  }

  "15 numbers as screen name" should "be declared valid" in {
    assertHandler("123456789012345", true)
  }

  "An screen name with all numbers and one leading letter" should "be declared valid" in {
    assertHandler("a6346367", true)
  }

  "An screen name with all numbers and one trailing letter" should "be declared valid" in {
    assertHandler("232634238b", true)
  }

  "An screen name with all numbers and one letter in the middle" should "be declared valid" in {
    assertHandler("6354273b273", true)
  }

  "An screen name with all numbers and a leading underscore" should "be declared valid" in {
    assertHandler("_4546385", true)
  }

  "An screen name with all numbers and a trailing underscore" should "be declared valid" in {
    assertHandler("7374904_", true)
  }

  "An screen name with all numbers and an underscore in the middle" should "be declared valid" in {
    assertHandler("34673_64647", true)
  }

  "An screen name that mixes upper and lower case letters" should "be declared valid" in {
    assertHandler("KbhjkbHJKhj", true)
  }

  "An screen name that mixes upper case letters and numbers" should "be declared valid" in {
    assertHandler("SHDS78D778", true)
  }

  "An screen name that mixes lower case letters and numbers" should "be declared valid" in {
    assertHandler("lkm34lkm3l4k", true)
  }

  "An screen name that mixes upper case, lower case letters and numbers" should "be declared valid" in {
    assertHandler("jkj2BNK87", true)
  }

  "Any screen name that includes an special character different from underscore (symbols, dashes, spaces ...)" should "be declared invalid" in {
    assertHandler("hgsg+d233", false)
  }

  "An email address" should "be a not valid screen name" in {
    assertHandler("mariano.navas@clluc.com", false)
  }

  "A normal screen name with the word twitter in it" should "be declared invalid" in {
    assertHandler("_rrtwitter", false)
  }

  "A normal screen name with the word admin in it" should "be declared invalid" in {
    assertHandler("533jjadmin", false)
  }

  "A normal screen name longer than 15 characters" should "be declared invalid" in {
    assertHandler("sjhbfsjdbsjdbcfjhskfjnskd", false)
  }
}
