package dawn.flow


case class Simulation[M](model: M, sinks: Seq[Sink[M]], scheduler: Scheduler) {

  def run() {
    scheduler.schedule(sinks, model)
  }

}

