package dawn.flow

import io.circe._
import io.circe.syntax._

object JsonExport {

  def apply[A:Encoder, B](s: Source[A, B]) = s.map((x: A) => x.asJson, "JsonExport")
  
}

case class PrintSink[A, B](source: Source[A, B]) extends Sink1[A, B] {
  def f(p: B, x: A) = println(x)
}

