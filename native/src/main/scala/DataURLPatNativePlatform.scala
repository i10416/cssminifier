package dev.i10416
import java.util.regex.Pattern
import java.util.regex.Matcher
trait DataURLPat {
  private final val dataURLStartFrom =
    java.util.regex.Pattern
      .compile("url\\(\\s*([\"']?)data\\:", Pattern.CASE_INSENSITIVE)
  def matchDataURLStart(str: String): Option[(Option[String], Int)] = {
    dataURLStartFrom.matcher(str) match {
      case DataURLPattern(Some(quoteLike), matchStartFrom) =>
        Some((Some(quoteLike), matchStartFrom))
      case DataURLPattern(None, matchStartFrom) => Some(None, matchStartFrom)
      case _                                    => None
    }
  }
}
