package dawn.flow

trait SourceN extends Node {
  self => 
  def scheduler: Scheduler
  lazy val sources: List[Source[_]] =
    rawSources.drop(1).foldLeft(List[Source[_]](rawSources(0)))(
      (acc, pos) => {
      if (acc(0).scheduler != pos.scheduler) {
        val s1: Source[_] = new ReplayWithScheduler(acc(0))
        val tl: List[Source[_]] = (acc ::: List(pos)).drop(1).map(x => new Replay(x, s1))
        s1 :: tl
      }
      else 
        acc ::: List(pos)
      }
    )

  def listenN(i: Int, x: Timestamped[_]) = ()
  override def setup() = {
    super.setup()
    sources.zipWithIndex.map { case (r, i) => r.addChannel(ChannelN(i+1, self, scheduler)) }
  }
  
}

trait Source0 extends SourceN {
  def schedulerHook: SchedulerHook
  def scheduler = schedulerHook.scheduler
  lazy val rawSources: List[Source[_]] = List()
  override lazy val sources: List[Source[_]] = List()
}

trait Source1[A] extends Node with SourceN { self =>

  def scheduler = source1.scheduler
  def nodeHook = rawSource1.nodeHook
  def rawSource1: Source[A]
  def source1: Source[A] = sources(0).asInstanceOf[Source[A]]
  override def rawSources: List[Source[_]] = List(rawSource1)
  def listen1(x: Timestamped[A])

  override def setup() = {
    super.setup()
    source1.addChannel(Channel1(self, scheduler))
  }
}

trait Source2[A, B] extends Source1[A] { self =>
  def rawSource2: Source[B]
  override def rawSources = super.rawSources ::: List(rawSource2)

  def listen2(x: Timestamped[B])

  def source2: Source[B] = sources(1).asInstanceOf[Source[B]]


  override def setup() = {
    super.setup()
    source2.addChannel(Channel2(self, scheduler))
  }
}

trait Source3[A, B, C] extends Source2[A, B] { self =>
  def rawSource3: Source[C]
  override def rawSources = super.rawSources ::: List(rawSource3)
  def listen3(x: Timestamped[C])

  def source3: Source[C] = sources(2).asInstanceOf[Source[C]]

  override def setup() = {
    super.setup()
    source3.addChannel(Channel3(self, scheduler))
  }

}

trait Source4[A, B, C, D] extends Source3[A, B, C] { self =>
  def rawSource4: Source[D]
  override def rawSources = super.rawSources ::: List(rawSource4)
  def listen4(x: Timestamped[D])

  def source4: Source[D] =
    sources(3).asInstanceOf[Source[D]]

  override def setup() = {
    super.setup()
    source4.addChannel(Channel4(self, scheduler))
  }

}

trait Source5[A, B, C, D, E] extends Source4[A, B, C, D] {
  self =>
  def rawSource5: Source[E]
  def listen5(x: Timestamped[E])
  override def rawSources = super.rawSources ::: List(rawSource5)
  def source5: Source[E] =
    sources(4).asInstanceOf[Source[E]]    

  override def setup() = {
    super.setup()
    source5.addChannel(Channel5(self, scheduler))
  }

}


trait Source6[A, B, C, D, E, F] extends Source5[A, B, C, D, E] {
  self =>
  def rawSource6: Source[F]
  def listen6(x: Timestamped[F])
  override def rawSources = super.rawSources ::: List(rawSource6)
  def source6: Source[F] =
    sources(5).asInstanceOf[Source[F]]    

  override def setup() = {
    super.setup()
    source6.addChannel(Channel6(self, scheduler))
  }

}
