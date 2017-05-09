package spatial.fusion.gen

import io.circe._
import io.circe.syntax._

case class JsonExport[A:Encoder]() extends Transformation[A, Json] {
  def process(x: A) = x.asJson
}

object JsonExport {
  def apply[A: Encoder](source: SourceStreamed[A]): TransformStreamed[A, Json] =
    Transform(source, JsonExport[A])
}

