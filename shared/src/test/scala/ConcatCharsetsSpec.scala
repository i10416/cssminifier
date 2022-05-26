package dev.i10416
import dev.i10416.CSSMinifier
class ConcatCharsetsSpec extends munit.FunSuite {
  // taken from https://github.com/yui/yuicompressor/blob/master/tests/concat-charset.css
  private val sample =
    """/* This is invalid CSS, but frequently happens as a result of concatenation. */
                          |@CHARSET "utf-8";
                          |#foo {
                          |	border-width:1px;
                          |}
                          |/*
                          |Note that this is erroneous!
                          |The actual CSS file can only have a single charset.
                          |However, this is the job of the author/application.
                          |The compressor should not get involved.
                          |*/
                          |@charset "another one";
                          |#bar {
                          |	border-width:10px;
                          |}
                          |""".stripMargin
  private val expected =
    "@charset \"utf-8\";#foo{border-width:1px}#bar{border-width:10px}"
  test("concatCharsets") {
    val obtained = CSSMinifier.run(sample)
    assertEquals(obtained, expected)
  }
}
