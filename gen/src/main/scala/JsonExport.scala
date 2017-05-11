package spatial.fusion.gen

import io.circe._
import io.circe.syntax._

case class JsonExport[A:Encoder, B](source: Source[A, B]) extends Map[A, Json, B] {
  def f(p: B, x: A) = x.asJson
}

