package dawn.flow

trait Batch[A, B] extends Source1T[A] with SourceT[B] with Accumulate1[Timestamped[A]] with CloseListener {

  def shClose = source1.sh

  override def closePriority = -1

  override lazy val sh = SecondaryScheduler()

  def f(lA: ListT[A]): ListT[B]

  def close() = {
    val lB = f(accumulated1)
    lB.foreach(x => sh.registerEvent(broadcast(x), x.time))
    sh.run()
  }

  //The default implementation on Source1 is on the wrong sh.
  //Not worth it to change all code since it once the first
  //scheduler is closed there is no effect possible
  source1.sh.executeBeforeStart({source1.addChannel(Channel1(this, source1.sh))})

}

case class Replay[A](source1: SourceT[A], shOut: Scheduler) extends Source1T[A] with SourceT[A] with Accumulate1[Timestamped[A]] with CloseListener {

  def name = "Replay"

  def shClose = source1.sh

  override def closePriority = 1
  override def sh = shOut

  def close() = {
    accumulated1.foreach(x => sh.registerEvent(broadcast(x), x.time))
  }

  //Same as above
  shClose.executeBeforeStart({source1.addChannel(Channel1(this, shClose))})  

}
