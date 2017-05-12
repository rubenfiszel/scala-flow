package dawn.flow

import io.circe._
import io.circe.syntax._

object JsonExport {

  def apply[A:Encoder, B](s: Source[A, B]) = s.map(_.asJson)
  
}

