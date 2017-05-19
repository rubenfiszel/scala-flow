package dawn.flow


case class Simulation[M](sinks: Seq[Sink[M]], scheduler: Scheduler = SimpleScheduler) {

  def reset() =
    Sourcable.collectResettable(sinks).map(_.reset())

  def run(model: M) = {
    reset()
    scheduler.schedule(sinks, model)
  }

}

