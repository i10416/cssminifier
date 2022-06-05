package dev.i10416
import dev.i10416.CSSMinifier
class BorderNoneSpec extends munit.FunSuite {
  // taken from https://github.com/yui/yuicompressor/blob/master/tests/concat-charset.css
  private val sample =
    """|
       |a {
       |    border : none ;
       |}
       |s {
       |    border-top : none ;
       |    border-right : none ;
       |    border-bottom : none ;
       |    border-left : none ;
       |    other: none ;
       |}""".stripMargin
  private val expected =
    "a{border:0}s{border-top:0;border-right:0;border-bottom:0;border-left:0;other:none}"
  test("compress border none") {
    val obtained = CSSMinifier.run(sample)
    assertEquals(obtained, expected)
  }
}
