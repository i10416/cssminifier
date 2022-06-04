package dev.i10416

trait DataURLPat {
  private final val dataURLStartFrom = "(?i)url\\(\\s*([\"']?)data\\:".r
  def matchDataURLStart(str: String): Option[(Option[String], Int)] = {
    dataURLStartFrom.findFirstMatchIn(str) match {
      case DataURLPattern(Some(quoteLike), matchStartFrom) =>
        Some((Some(quoteLike), matchStartFrom))
      case DataURLPattern(None, matchStartFrom) => Some(None, matchStartFrom)
      case _                                    => None
    }
  }
}
