package dawn.flow


case class Simulation[M](model: M, sinks: Seq[Sink[M]], scheduler: Scheduler) {

  def reset() =
    sinks.foreach(_.reset)

  def run() {
  reset()    
    scheduler.schedule(sinks, model)
  }

}

