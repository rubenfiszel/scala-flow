package dawn.flow


case class Simulation[M](model: M, sinks: Seq[Sink[M]], scheduler: Scheduler) {

  def reset() =
    Sourcable.collectResettable(sinks).map(_.reset())

  def run() = {
    reset()
    scheduler.schedule(sinks, model)
  }

}

