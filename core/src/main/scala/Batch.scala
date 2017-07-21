package dawn.flow

trait Batch[A, R]
    extends Source1[A]
    with Source[R]
    with Accumulate[A]
    with CloseListener {

  def schedulerClose = source1.scheduler

  override def closePriority = -1

  //defined by Accumulate
  def listen1(x: Timestamped[A]) = ()

  private var schedulerL: Scheduler = Scheduler.newOne()
  override def scheduler: Scheduler = {
    schedulerL
  }

  def f(lA: ListT[A]): ListT[R]

  var numClosed = 0
  var numClosedRequired: Int = 1
  def onScheduleClose() = {
    numClosed += 1
    if (numClosed == numClosedRequired) {
      val lR = f(accumulated(0))
      lR.foreach(x => scheduler.registerEvent(broadcast(x), x.time))
      scheduler.run()
    }
  }

  //The default implementation on Source1 is on the wrong ch.
  //Not worth it to change all code since it once the first
  //scheduler is closed there is no effect possible
  override def setup() = {
    super.setup()
    source1.addChannel(Channel1(this, source1.scheduler))
    source1.addChannel(ChannelN(1, this, source1.scheduler))    
  }

  override def reset() = {
    super.reset()
    numClosed = 0
    schedulerL = Scheduler.newOne()
  }

}



object Batch {
  def apply[A, B](rawSource11: Source[A],
                  f1: ListT[A] => ListT[B],
                  name1: String = "Batch") = new Batch[A, B] {
    def rawSource1 = rawSource11
    def name = name1
    def f(x: ListT[A]) = f1(x)
  }
}

class ReplayWithScheduler[A](val rawSource1: Source[A]) extends Batch[A, A] {
  def name = "Replay w/ scheduler"
  def f(lA: ListT[A]) = lA

  override lazy val sources = rawSources
//  override def numClosedRequired = 2
}

class Replay[A](val rawSource1: Source[A], sourceScheduler: ReplayWithScheduler[_])
    extends Source1[A]
    with Source[A]
    with Accumulate[A]
    with CloseListener {

  def name = "Replay"

  def schedulerClose = rawSource1.scheduler


  def listen1(x: Timestamped[A]) = ()

  //clever trick to make topological sort order depend on the sourceOut schedule
  override def rawSources = sourceScheduler :: super.rawSources

  override lazy val sources = rawSources

  override def closePriority = 1
  override def scheduler = sourceScheduler.scheduler

  def onScheduleClose() = {
    accumulated(0).foreach(x => scheduler.registerEvent(broadcast(x), x.time))
  }

  //Same as above
  override def setup() = {
    schedulerClose.addCloseListener(sourceScheduler)
    sourceScheduler.numClosedRequired += 1
    rawSource1.addChannel(ChannelN(1, this, rawSource1.scheduler))    
    super.setup()
  }

}
