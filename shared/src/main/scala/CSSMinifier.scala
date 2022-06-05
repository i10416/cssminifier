package dev.i10416

import scala.annotation.tailrec
import scala.collection.immutable
import scala.util.matching.Regex
import scala.collection.mutable
import scala.scalajs.js.annotation.JSExportTopLevel
import dev.i10416.DataURLPat
import java.util.regex.Matcher
import scala.util.matching.Regex.Match

@JSExportTopLevel("CSSMinifier")
object CSSMinifier extends DataURLPat {
  object CharMatcher {
    def openComment = (prev: Option[Char], next: Char) =>
      (prev, next) match {
        // note: context must not be InsideStringLiteral
        case (Some('/'), '*') => true
        case _                => false
      }
    def openCommentOrString = (prev: Option[Char], next: Char) =>
      (prev, next) match {
        // note: context must not be InsideStringLiteral
        case (Some('/'), '*')   => true
        case (Some('\\'), '\"') => false
        case (Some(_), '\"')    => true
        case _                  => false
      }
    def closeComment = (prev: Option[Char], next: Char) =>
      (prev, next) match {
        case (Some('*'), '/') => true
        case _                => false
      }
    def closeString = (q: Char) =>
      (prev: Option[Char], next: Char) =>
        (prev, next) match {
          case (Some('\\'), `q`) => false
          case (Some(_), `q`)    => true
          case _                 => false
        }

  }

  /** generate placeholder to preserve contents in original css
    */
  private def placeholder(label: String, n: Int) =
    s"____PRESERVED_${label.toUpperCase()}_TOKEN__${n}___"
  final val startComment = "/\\*".r

  /** Leave data urls alone to increase parse performance.
    */
  @tailrec
  def preserveURLs(
      s: String,
      done: mutable.StringBuilder = new StringBuilder(),
      preservedURLs: List[String] = Nil
  ): (String, List[String]) = {
    matchDataURLStart(s) match {
      case Some((Some(quoteLike), startPos)) => {
        val quote = if (quoteLike == "\"") '\"' else '\'' // " or '
        val startFrom = startPos + 4 // skip `url(<quote>`
        val (leading, trailing) = s.splitAt(startFrom)
        // `leading` contains `url("` at the end of string
        val (url, tail) =
          readUntilClosingPairOrEOF(trailing.drop(1).toList, quote) match {
            // `remains` start from char after closing char, do NOT contain `"` nor `'`.
            case Some((dataURL, remains)) => (dataURL.trim(), remains.mkString)
            case None =>
              ??? // Left(parse error while processing data url. expect ')' but EOF)
          }
        done.append(leading)
        done.append(placeholder("URL", preservedURLs.length))
//        done.append(quote)
        preserveURLs(tail, done, ("\"" + url + "\"") :: preservedURLs)
      }

      case Some((None, startPos)) => {
        val close = ')'
        val startFrom = startPos + 4 // skip `url(`
        val (leading, trailing) = s.splitAt(startFrom)
        val (url, tail) =
          readUntilClosingPairOrEOF(trailing.toList, close) match {
            // remains start from just after `)`, do NOT contain `)`.
            case Some((dataURL, remains)) => (dataURL.trim(), remains.mkString)
            case None                     => ??? // Left(expect ')' but EOF)
          }
        done.append(leading)
        done.append(placeholder("URL", preservedURLs.length))
        done.append(close)
        println(tail)
        preserveURLs(tail, done, url :: preservedURLs)
      }
      case None =>
        done.addAll(s)
        (done.toString(), preservedURLs.reverse)
    }
  }

  // leading part contains between [0,cursor].
  // e.g. split "string" where prev == 'r' and next == 'i' result in (3,[s,t,r,i],[n,g])
  @tailrec
  def splitWhereAfter(
      str: List[Char],
      matcher: (Option[Char], Char) => Boolean,
      cursor: Int = 0,
      consumed: List[Char] = Nil,
      prev: Option[Char] = None
  ): (Int, List[Char], List[Char]) = {
    str match {
      case head :: tail if matcher(prev, head) =>
        (cursor, (head :: consumed).reverse, tail)
      case head :: tail =>
        splitWhereAfter(tail, matcher, cursor + 1, head :: consumed, Some(head))
      case Nil => (cursor, consumed.reverse, Nil)
    }
  }

  // leading part contains [0,cursor].
  // e.g. split "string" where prev == 'r' and next == 'i' result in (3,[s,t][r,i,n,g])
  @tailrec
  def splitWhereBefore(
      str: List[Char],
      matcher: (Option[Char], Char) => Boolean,
      cursor: Int = 0,
      consumed: List[Char] = Nil,
      prev: Option[Char] = None,
      shouldReverseBack: Boolean = true
  ): (Int, List[Char], List[Char]) = {
    str match {
      case head :: tail if matcher(prev, head) =>
        val (prev :: other) = consumed
        (
          cursor,
          if (shouldReverseBack) other.reverse else other,
          prev :: head :: tail
        )
      case head :: tail =>
        splitWhereBefore(
          tail,
          matcher,
          cursor + 1,
          head :: consumed,
          Some(head),
          shouldReverseBack
        )
      case Nil =>
        (cursor, if (shouldReverseBack) consumed.reverse else consumed, Nil)
    }
  }

  /** remove redundant leading and trailing whitespace-like chars, repeated
    * semi-colons and empty rules
    */
  @tailrec
  def handleEmptyLike(
      str: List[Char],
      prev: Option[Char] = None,
      result: List[Char] = Nil
  ): List[Char] = {
    (str, prev) match {
      // normalize repeated whitespace-like chars into single whitespace char.
      // e.g. .klass  {
      //     foo   : bar
      // }
      // => .klass {foo : bar}
      case (repeated :: tail, Some(_prev))
          if repeated.isWhitespace && _prev.isWhitespace =>
        handleEmptyLike(tail, prev, result)
      // some characters are not affected after removing leading whitespace
      case (
            (char @ ('=' | '{' | '(' | ')' | '}' | ';' | ',' | '>' |
            '[')) :: tail,
            Some(ws)
          ) if ws.isWhitespace =>
        val _ :: remains = result
        handleEmptyLike(tail, Some(char), char :: remains)
      // remove trailing space after some characters
      case (
            ws :: tail,
            Some(
              char @ (';' | '{' | '}' | '\r' | '\n' | '=' | '>' | '!' | '(' |
              '[' | ',')
            )
          ) if ws.isWhitespace =>
        handleEmptyLike(tail, prev, result)
      // ignore repeated semi colons as it is meaningless
      // e.g. ;;; => ;
      case (';' :: tail, Some(';')) =>
        handleEmptyLike(tail, prev, result)
      // semi-colon before closing brace can be omitted
      // e.g. ;} => }
      case ('}' :: tail, Some(';')) =>
        val _ :: remains = result
        handleEmptyLike(tail, Some('}'), '}' :: remains)
      // remove new lines
      case (('\r' | '\n') :: tail, Some(_)) =>
        handleEmptyLike(tail, prev, result)
      // remove empty rules
      // e.g `abc {}` => ``
      case ('}' :: tail, Some('{')) =>
        result
          .drop(1) // remove `{`
          // go back
          .dropWhile(c => c != '{' && c != '}' && c != ';' && c != '/') match {
          case head :: next =>
            handleEmptyLike(tail, Some(head), head :: next)
          case Nil => handleEmptyLike(tail, None, Nil)
        }
      case (head :: tail, prev) =>
        handleEmptyLike(tail, Some(head), head :: result)
      case (Nil, _) => result.reverse
    }
  }
  def handleZeros = {}
  def handleColors = {}

  /** Assume s is a complete css content or valid css content just after comment
    * block; This function removes comments except ones starting with `!`,
    * preserve comments starts with `!`, string literals and the fisrt charset.
    *
    * @return
    *   (text without comments and strings to be removed,comments to be
    *   preserved,strings to be preserved,charset)
    */
  @tailrec
  def handleCommentsAndStrings(
      s: List[Char],
      done: StringBuilder = new StringBuilder,
      preservedComments: List[String] = Nil,
      preservedStrings: List[String] = Nil,
      charset: Option[String] = None
  ): (String, List[String], List[String], Option[String]) = {
    splitWhereBefore(
      s,
      CharMatcher.openCommentOrString,
      shouldReverseBack = false
    ) match {
      // consumed all chars and there remains no comment start nor string literal start.
      case (_, withoutComments, Nil) =>
        done.appendAll(withoutComments.reverse)
        (
          done.toString().trim,
          preservedComments.reverse,
          preservedStrings.reverse,
          charset
        )
      // avoid mistakenly remove comment-like value from string
      case (
            _,
            consumed,
            // we are sure char before quote char here won't escape quote
            prevQuote :: (q @ ('\"' | '\'')) :: startStringLiteral
          ) =>
        val (_, stringPart, remains) =
          splitWhereAfter(startStringLiteral, CharMatcher.closeString(q))

        /** Authors using an @charset rule must place the rule at the very
          * beginning of the style sheet, __preceded by no characters__.
          * @charset
          *   must be lowercase, no backslash escapes, followed by the encoding
          *   name, followed by ";".
          */
        // handle charset here as charset contains string literal
        if (
          prevQuote.isWhitespace && q == '\"' && (consumed.startsWith(
            "tesrahc@"
          ) || consumed.startsWith("TESRAHC@")) && remains.startsWith(";")
        ) {
          done.appendAll(
            consumed.drop(8).reverse
          ) // drop `@charset` from consumed string
          handleCommentsAndStrings(
            remains.drop(1), // remove semicolon
            done,
            preservedComments,
            preservedStrings,
            charset.fold(Some(s"@charset \"${stringPart.mkString};"))(c =>
              Some(c.toLowerCase)
            )
          )
        } else {
          done.appendAll(consumed.reverse)
          done.append(prevQuote)
          done.append(q)
          done.appendAll(placeholder("STRING", preservedStrings.length))
          done.append(q)
          // `/*` inside string literal should not be regarded as start signal of comment part.
          // thus, must not be removed from processed output.
          handleCommentsAndStrings(
            remains,
            done,
            preservedComments,
            stringPart.dropRight(1).mkString :: preservedStrings,
            charset
          )
        }
      case (_, untilComment, fromComment @ ('/' :: '*' :: comment)) =>
        done.appendAll(untilComment.reverse)
        // remove leading `/*     `
        fromComment.drop(2).dropWhile(_.isWhitespace) match {
          case '!' :: needPreserve => // e.g. `/*      !KEEP THIS COMMENT... `
            splitWhereAfter(needPreserve, CharMatcher.closeComment) match {
              case (_, comment, remains) =>
                done.appendAll("/*!")
                // preserve comment
                // remains can be Nil if there is no close pattern, but it is Ok because
                // we regard the all remaining part as a comment.
                done.append(placeholder("COMMENT", preservedComments.length))
                done.appendAll("*/")
                // spaces between end of a comment and start of next element can be removed;
                handleCommentsAndStrings(
                  remains.dropWhile(_.isWhitespace),
                  done,
                  comment.dropRight(2).mkString :: preservedComments,
                  preservedStrings,
                  charset
                )
            }
          case _ =>
            splitWhereAfter(
              fromComment.drop(2),
              CharMatcher.closeComment
            ) match {
              case (_, _, remains) =>
                // _,....*/,remains
                // discard comment part and continue
                // spaces between end of a comment and start of next element can be removed;
                handleCommentsAndStrings(
                  remains.dropWhile(_.isWhitespace),
                  done,
                  preservedComments,
                  preservedStrings,
                  charset
                )
            }
        }
      case (_, withoutComments, _) =>
        done.appendAll(withoutComments.reverse)
        (
          done.toString().trim,
          preservedComments.reverse,
          preservedStrings.reverse,
          charset
        )
    }
  }

  def run(css: String): String = {
    val (cssWithoutDataURL, preservedURLs) = preserveURLs(css)
    // maybe we should return cssWithoutDataURL as List[Char] to reduce conversion

    // before compress ,we need to preserve strings to avoid accidentally
    // minifying string like "...\*................*\..."
    val (withoutComments, preservedComments, preservedStrings, charset) =
      handleCommentsAndStrings(
        cssWithoutDataURL.toList
      )
    // handleZeros
    // handleColors
    val s0 = handleEmptyLike(withoutComments.toList).mkString
    // put urls back
    val s1 = preservedURLs.zipWithIndex.foldLeft(s0) { case (acc, (url, idx)) =>
      acc.replace(placeholder("URL", idx), url)
    }
    // put comments back
    val s2 = preservedComments.zipWithIndex.foldLeft(s1) {
      case (acc, (comment, idx)) =>
        acc.replace(placeholder("COMMENT", idx), comment)
    }
    // put strings back
    charset.getOrElse("") ++ preservedStrings.zipWithIndex
      .foldLeft(s2) { case (acc, (str, idx)) =>
        acc.replace(placeholder("STRING", idx), str)
      }
      .replaceAll(":0 0 0 0(;|})", ":0$1") // Replace 0 0 0 0; with 0.
      .replaceAll(":0 0 0(;|})", ":0$1") // Replace 0 0 0; with 0.
    // .replaceAll("(?<!flex):0 0(;|})", ":0$1")
  }

  @tailrec
  def readUntilClosingPairOrEOF(
      s: List[Char],
      close: Char,
      result: StringBuilder = new StringBuilder(),
      prev: Option[Char] = None
  ): Option[(String, List[Char])] = {
    (s, prev) match {
      case (Nil, None)    => None
      case (Nil, Some(c)) => None
      // continue if closing char is escaped
      case (`close` :: tail, Some('\\')) =>
        result.append(close)
        readUntilClosingPairOrEOF(tail, close, result, Some(close))
      case (`close` :: tail, Some(_)) =>
        Some((result.toString(), tail))
      case (head :: tail, Some(_)) =>
        result.append(head)
        readUntilClosingPairOrEOF(tail, close, result, Some(head))
      case (head :: tail, None) =>
        result.append(head)
        readUntilClosingPairOrEOF(tail, close, result, Some(head))
    }
  }
}

object DataURLPattern {
  def unapply(m: Matcher): Option[(Option[String], Int)] = {
    if (m.matches) {
      m.groupCount match {
        case 0 => None
        case 1 => Some(None, m.start)
        case 2 => Some(Some(m.group(1)), m.start)
        case _ => None
      }
    } else {
      None
    }
  }
  def unapply(m: Option[Match]): Option[(Option[String], Int)] = {
    m match {
      case None => None
      case Some(urlWithoutQuotes) if urlWithoutQuotes.group(1).isEmpty =>
        Some(None, urlWithoutQuotes.start)
      case Some(urlWithQuoteLike) =>
        Some(Some(urlWithQuoteLike.group(1)), urlWithQuoteLike.start)
    }
  }
}
