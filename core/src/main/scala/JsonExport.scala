package dawn.flow

import io.circe._
import io.circe.syntax._


object JsonExport {

  def apply[A:Encoder](s: Source[A]) = s.map((x: A) => x.asJson, "JsonExport")
  
}

case class PrintSink[A](rawSource1: Source[A])(implicit val sourcableHook: SourcableHook) extends Sink1[A] {
  def name = "PrintSink"
  def f(x: A) = println(x)
}

