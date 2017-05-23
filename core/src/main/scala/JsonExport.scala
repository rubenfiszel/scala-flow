package dawn.flow

import io.circe._
import io.circe.syntax._

object JsonExport {

  def apply[A:Encoder](s: Source[A]) = s.map((x: A) => x.asJson, "JsonExport")
  
}

case class PrintSink[A](source: Source[A]) extends Sink1[A] {
  def f(x: A) = println(x)
}

