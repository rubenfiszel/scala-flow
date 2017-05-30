package dawn.flow

trait Source0 extends Node {
  lazy val sources: List[Source[_]] = List()
}

trait Source1[A] extends Node { self =>

  def scheduler: Scheduler = source1.scheduler
  def rawSource1: Source[A]
  def source1: Source[A] = rawSource1
  override def sources: List[Node] = List(source1)
  def listen1(x: Timestamped[A])

  //Memoization to avoid recreating clone
  lazy val os1: collection.mutable.Map[(Source[A], Source[_]), Source[A]] =
    collection.mutable.Map()

  def overrideSource1[C](lSource1: Source[A], lSourceX: Source[C]) = {
    os1.getOrElseUpdate((lSource1, lSourceX), {
      if (lSource1.scheduler != lSourceX.scheduler) {
        ReplayWithScheduler(lSource1)
      } else
        lSource1
    })
  }

  lazy val os2: collection.mutable.Map[(Source[A], Source[_]), Source[_]] =
    collection.mutable.Map()

  def overrideSourceX[C](lSource1: Source[A], lSourceX: Source[C]) = {
    os2
      .getOrElseUpdate((lSource1, lSourceX), {
        if (lSource1.scheduler != lSourceX.scheduler)
          Replay(lSourceX, lSource1.scheduler)
        else
          lSourceX
      })
      .asInstanceOf[Source[C]]
  }

  scheduler.executeBeforeStart(source1.addChannel(Channel1(self, scheduler)))
}

trait Source2[A, B] extends Source1[A] { self =>
  def rawSource2: Source[B]
  override def sources = source2 :: super.sources
  def listen2(x: Timestamped[B])

  override def source1: Source[A] =
    overrideSource1(super.source1, rawSource2)

  def source2: Source[B] =
    overrideSourceX(source1, rawSource2)

  scheduler.executeBeforeStart(source2.addChannel(Channel2(self, scheduler)))
}

trait Source3[A, B, C] extends Source2[A, B] { self =>
  def rawSource3: Source[C]
  override def sources = source3 :: super.sources
  def listen3(x: Timestamped[C])

  override def source1: Source[A] =
    overrideSource1(super.source1, rawSource3)

  override def source2: Source[B] =
    overrideSourceX(source1, super.source2)

  def source3: Source[C] =
    overrideSourceX(source1, rawSource3)

  scheduler.executeBeforeStart(source3.addChannel(Channel3(self, scheduler)))

}

trait Source4[A, B, C, D] extends Source3[A, B, C] { self =>
  def rawSource4: Source[D]
  override def sources = source4 :: super.sources
  def listen4(x: Timestamped[D])

  override def source1: Source[A] =
    overrideSource1(super.source1, rawSource4)

  override def source2: Source[B] =
    overrideSourceX(source1, super.source2)

  override def source3: Source[C] =
    overrideSourceX(source1, super.source3)

  def source4: Source[D] =
    overrideSourceX(source1, rawSource4)

  scheduler.executeBeforeStart(source4.addChannel(Channel4(self, scheduler)))

}
