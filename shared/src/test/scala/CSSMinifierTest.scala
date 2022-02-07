package dev.i10416
import dev.i10416.CSSMinifier
import dev.i10416.CSSMinifier.CharMatcher

class CSSMinifierTest extends munit.FunSuite {
  private def sample = s"""|
                   |/* this is a comment to be removed */
                   |
                   |.klass foo bar {
                   |  .also .empty  {
                   |    
                   |  }
                   |}
                   |
                   |@charset "utf-8";
                   |
                   |
                   |value = "/* this 'must Not\\" be removed!!*/"
                   |
                   |/*! This is an important comment not to be removed!!*/
                   |
                   |@charset "utf-16";
                   |
                   |
                   |/* this is a comment to be removed */
                   |
                   |.empty .rule {   }
                   |
                   |@charset "uft-32";
                   |
                   |.has rule {
                   |  font-size: 16px;
                   |}
                   |other line;;
                   |
                   |
                   |/*! This is another important comment not to be removed!!*/
                   |
                   |""".stripMargin
  test(
    "helper:splitWhereAfter splits input string at where matcher matches, and returns leading part, which contains matched chars, and remaining part"
  ) {
    val (_, leading0, trailing0) = CSSMinifier.splitWhereAfter(
      "string".toList,
      (prev, next) => prev == Some('r') && next == 'i'
    )
    assertEquals(leading0, List('s', 't', 'r', 'i'))
    assertEquals(trailing0, List('n', 'g'))
  }
  test("helper:splitWhereAfter returns (as is,Nil) if there's no match)") {
    val input = "string"
    val (_, leading1, trailing1) = CSSMinifier.splitWhereAfter(
      input.toList,
      (prev, next) => false
    )
    assertEquals(leading1, input.toList)
    assertEquals(trailing1, Nil)
  }
  test(
    "helper:splitWhereBefore splits input string at where matcher matches and returns leading part, which contains matched chars, and remaining part)"
  ) {
    val (_, leading0, trailing0) = CSSMinifier.splitWhereBefore(
      "string".toList,
      (prev, next) => prev == Some('r') && next == 'i'
    )
    assertEquals(leading0, List('s', 't'))
    assertEquals(trailing0, List('r', 'i', 'n', 'g'))
  }
  test("helper:splitWhereBefore returns (as is,Nil) if there's no match)") {
    val input = "string"
    val (_, leading1, trailing1) = CSSMinifier.splitWhereBefore(
      input.toList,
      (prev, next) => false
    )
    assertEquals(leading1, input.toList)
    assertEquals(trailing1, Nil)
  }
  test(
    "helper:splitWhereBefore(str,openComment) split input at where `/*` exists"
  ) {
    val commentExistsAtTheMiddle =
      "this is leading text. /* start comment from here ...... And end here.*/ this is trailing text."
    val (_, beforeComment, comment) = CSSMinifier.splitWhereBefore(
      commentExistsAtTheMiddle.toList,
      CharMatcher.openComment
    )
    assert(comment.startsWith(Seq('/', '*')))
    assertEquals(beforeComment ::: comment, commentExistsAtTheMiddle.toList)
    val commentExistsAtTheStart =
      "/* comment start from here!... and end here */ This is trailing text."
    val (_, beforeComment1, comment1) = CSSMinifier.splitWhereBefore(
      commentExistsAtTheStart.toList,
      CharMatcher.openComment
    )
    assert(comment1.startsWith(Seq('/', '*')))
    assertEquals(beforeComment1, Nil)
  }
  test(
    "helper:splitWhereBefore(str,openComment) does not match invalid comment"
  ) {
    val invalidCommentExistsAtTheMiddle =
      "this is leading text. / *start comment from here ...... And end here.*/ this is trailing text."
    val (_, beforeComment, comment) = CSSMinifier.splitWhereBefore(
      invalidCommentExistsAtTheMiddle.toList,
      CharMatcher.openComment
    )
    assertEquals(beforeComment.toList, invalidCommentExistsAtTheMiddle.toList)
    val invalidCommentExistsAtTheStart =
      "/ * comment start from here!... and end here */ This is trailing text."
    val (_, beforeComment1, comment1) = CSSMinifier.splitWhereBefore(
      invalidCommentExistsAtTheStart.toList,
      CharMatcher.openComment
    )
    assertEquals(beforeComment1.toList, invalidCommentExistsAtTheStart.toList)
  }
  test(
    "helper:splitWhereBefore(str,closeComment) split input at where `*/` exists"
  ) {
    val commentExistsAtTheMiddle =
      "this is leading text. /* start comment from here ...... And end here.*/ this is trailing text."
    val (_, beforeComment, comment) = CSSMinifier.splitWhereBefore(
      commentExistsAtTheMiddle.toList,
      CharMatcher.closeComment
    )
    assert(comment.startsWith(Seq('*', '/')))
    assertEquals(beforeComment ::: comment, commentExistsAtTheMiddle.toList)
    val commentExistsAtTheStart = "*/ This is trailing text."
    val (_, beforeComment1, comment1) = CSSMinifier.splitWhereBefore(
      commentExistsAtTheStart.toList,
      CharMatcher.closeComment
    )
    assert(comment1.startsWith(Seq('*', '/')))
    assertEquals(beforeComment1, Nil)
  }
  test(
    "helper:splitWhereBefore(str,closeComment) does not match invalid comment"
  ) {
    val invalidCommentExistsAtTheMiddle =
      "this is leading text. /*start comment from here ...... And end here.* / this is trailing text."
    val (_, beforeComment, comment) = CSSMinifier.splitWhereBefore(
      invalidCommentExistsAtTheMiddle.toList,
      CharMatcher.closeComment
    )
    assertEquals(beforeComment.toList, invalidCommentExistsAtTheMiddle.toList)
    val invalidCommentExistsAtTheStart =
      "/ * comment start from here!... and end here * / This is trailing text."
    val (_, beforeComment1, comment1) = CSSMinifier.splitWhereBefore(
      invalidCommentExistsAtTheStart.toList,
      CharMatcher.closeComment
    )
    assertEquals(beforeComment1.toList, invalidCommentExistsAtTheStart.toList)
  }
  test("handleEmptyLike removes empty rules") {
    val res = CSSMinifier.handleEmptyLike(sample.toList)
    assert(!res.mkString.contains(".klass foo bar"))
    assert(!res.mkString.contains(".empty rule"))
    assert(res.mkString.contains(".has rule"))

  }
  test("handleEmptyLike removes repeated semi-colon ") {
    val res = CSSMinifier.handleEmptyLike(sample.toList)
    assert(res.mkString.contains(";"))
    assert(!res.mkString.contains(";;"))
    assert(!res.mkString.contains(";}"))
  }
 /* test("handleComments removes comments") {
    val (res, preserved) = CSSMinifier.compressComments(sample.toList)
    assert(!res.mkString.contains("/* this is a comment to be removed */"))
    assert(res.mkString.contains("/*____PRESERVED_COMMENT_TOKEN__0___*/"))
    assert(res.mkString.contains("/*____PRESERVED_COMMENT_TOKEN__1___*/"))
    assertEquals(
      preserved.head,
      " This is an important comment not to be removed!!"
    )
    assertEquals(
      preserved.apply(1),
      " This is another important comment not to be removed!!"
    )

  }*/
  test("handleCharset keeps only the first @charset declaration") {
    val res = CSSMinifier.handleCharsets(""".klass{};.other.klass{};@charset "utf-8"; .klass {foo:1};@charset "utf-16";.other .klass {};@charset "utf-32"; """.toList)
    assert(res.startsWith("""@charset "utf-8";"""))
    assert(!res.contains(""""@charset "utf-16";""""))
    assert(!res.contains(""""@charset "utf-32";""""))

  }
}
