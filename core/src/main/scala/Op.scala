package dawn.flow

sealed trait Op[A] extends Source[A]
trait Op1[A, B] extends Op[B] with Source1[A]
trait Op2[A, B, C] extends Op[C] with Source2[A, B]
trait Op3[A, B, C, D] extends Op[D] with Source3[A, B, C]
trait Op4[A, B, C, D, E] extends Op[E] with Source4[A, B, C, D]
