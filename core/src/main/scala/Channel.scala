package dawn.flow

sealed trait Channel[A] {
  def push(x: Timestamped[A], dt: Time): Unit
}

case class Channel1[A](receiver: Source1[A], scheduler: Scheduler)
    extends Channel[A] {
  def push(x: Timestamped[A], dt: Time = 0) = {
    scheduler.executeIn(receiver.listen1(x), dt)
  }
}

case class Channel2[A](receiver: Source2[_, A], scheduler: Scheduler)
    extends Channel[A] {
  def push(x: Timestamped[A], dt: Time = 0) =
    scheduler.executeIn(receiver.listen2(x), dt)
}

case class Channel3[A](receiver: Source3[_, _, A], scheduler: Scheduler)
    extends Channel[A] {
  def push(x: Timestamped[A], dt: Time = 0) =
    scheduler.executeIn(receiver.listen3(x), dt)
}

case class Channel4[A](receiver: Source4[_, _, _, A], scheduler: Scheduler)
    extends Channel[A] {
  def push(x: Timestamped[A], dt: Time = 0) =
    scheduler.executeIn(receiver.listen4(x), dt)
}

case class Channel5[A](receiver: Source5[_, _, _, _, A], scheduler: Scheduler)
    extends Channel[A] {
  def push(x: Timestamped[A], dt: Time = 0) =
    scheduler.executeIn(receiver.listen5(x), dt)
}
