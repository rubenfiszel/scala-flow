package dawn.flow

trait Batch[A, B] extends Source1[A] with Source[B] with Accumulate1[A] with CloseListener {

  def schedulerClose = source1.scheduler

  override def closePriority = -1

  override lazy val scheduler = SecondaryScheduler()

  def f(lA: ListT[A]): ListT[B]

  def close() = {
    val lB = f(accumulated1)
    lB.foreach(x => scheduler.registerEvent(broadcast(x), x.time))
    scheduler.run()
  }

  //The default implementation on Source1 is on the wrong sh.
  //Not worth it to change all code since it once the first
  //scheduler is closed there is no effect possible
  source1.scheduler.executeBeforeStart({source1.addChannel(Channel1(this, source1.scheduler))})

}

case class ReplayWithScheduler[A](rawSource1: Source[A]) extends Batch[A, A] {
  def name = "Replay w/ scheduler"
  def f(lA: ListT[A]) = lA
}


case class Replay[A](rawSource1: Source[A], schedulerOut: Scheduler) extends Source1[A] with Source[A] with Accumulate1[A] with CloseListener {

  def name = "Replay"

  def schedulerClose = source1.scheduler

  override def closePriority = 1
  override def scheduler = schedulerOut

  def close() = {
    accumulated1.foreach(x => scheduler.registerEvent(broadcast(x), x.time))
  }

  //Same as above
  schedulerClose.executeBeforeStart({source1.addChannel(Channel1(this, schedulerClose))})  

}
