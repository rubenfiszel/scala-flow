package spatial.fusion.gen

import io.circe._
import io.circe.syntax._

case class JsonExport[A:Encoder](source: Source[A]) extends Map[A, Json] {
  def f(x: A) = x.asJson
}

