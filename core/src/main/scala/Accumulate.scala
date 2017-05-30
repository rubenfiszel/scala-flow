package dawn.flow

trait Accumulate1[A] {
  var accumulated1: ListT[A] = List()
  def listen1(x: Timestamped[A]) = {
    accumulated1 ::= x
  }
}

trait Accumulate2[A, B] extends Accumulate1[A] {
  var accumulated2: ListT[B] = List()
  def listen2(x: Timestamped[B]) = {
    accumulated2 ::= x
  }
}
