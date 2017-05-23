package dawn.flow


case class Simulation[M](sinks: Seq[Sink], scheduler: Scheduler = SimpleScheduler)(implicit mc: ModelCallBack[M]) {

  def reset() =
    Sourcable.collectResettable(sinks).map(_.reset())

  def run(model: M) = {
    reset()
    mc(model)
    scheduler.schedule(sinks)
  }

}

