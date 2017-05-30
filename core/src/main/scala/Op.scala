package dawn.flow

trait Op1[A, B] extends Source[B] with Source1[A]
trait Op2[A, B, C] extends Source[C] with Source2[A, B]
trait Op3[A, B, C, D] extends Source[D] with Source3[A, B, C]
trait Op4[A, B, C, D, E] extends Source[E] with Source4[A, B, C, D]
