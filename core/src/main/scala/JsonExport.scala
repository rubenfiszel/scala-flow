package dawn.flow

import io.circe._
import io.circe.syntax._

object JsonExport {

  def apply[A: Encoder](s: Source[A]) = s.map((x: A) => x.asJson, "JsonExport")

}

case class PrintSink[A](rawSource1: Source[A])(implicit val nodeHook: NodeHook)
    extends Sink1[A] {
  def name = "PrintSink"
  def f(x: Timestamped[A]) = println(x)
}
