package dawn.flow

trait Batch[A, B]
    extends Source1[A]
    with Source[B]
    with Accumulate1[A]
    with CloseListener {

  def schedulerClose = source1.scheduler

  override def closePriority = -1

  private var schedulerL: Scheduler = Scheduler.newOne()
  override def scheduler: Scheduler = {
    schedulerL
  }

  def f(lA: ListT[A]): ListT[B]

  def onScheduleClose() = {
    val lB = f(accumulated1)
    lB.foreach(x => scheduler.registerEvent(broadcast(x), x.time))
    scheduler.run()
  }

  //The default implementation on Source1 is on the wrong sh.
  //Not worth it to change all code since it once the first
  //scheduler is closed there is no effect possible
  override def setup() = {
    schedulerL = new Scheduler {}
    source1.scheduler.childSchedulers ::= scheduler
    source1.addChannel(Channel1(this, source1.scheduler))
    super.setup()    
  }

}


object Batch {
  def apply[A, B](rawSource11: Source[A], f1: ListT[A] => ListT[B], name1: String = "Batch") = new Batch[A, B] {
    def rawSource1 = rawSource11
    def name = name1
    def f(x: ListT[A]) = f1(x)
  }
}

case class ReplayWithScheduler[A](rawSource1: Source[A]) extends Batch[A, A] {
  def name = "Replay w/ scheduler"
  def f(lA: ListT[A]) = lA
}

case class Replay[A](rawSource1: Source[A], sourceOut: Source[_])
    extends Source1[A]
    with Source[A]
    with Accumulate1[A]
    with CloseListener {

  def name = "Replay"

  def schedulerClose = source1.scheduler


  //clever trick to make topological sort order depend on the sourceOut schedule
  override def sources = sourceOut :: super.sources

  override def closePriority = 1
  override def scheduler = sourceOut.scheduler

  def onScheduleClose() =
    accumulated1.foreach(x => scheduler.registerEvent(broadcast(x), x.time))

  //Same as above
  override def setup() = {
    source1.addChannel(Channel1(this, source1.scheduler))    
    super.setup()
  }

}
