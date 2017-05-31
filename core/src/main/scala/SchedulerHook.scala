package dawn.flow

case object FakeScheduler extends Scheduler

case class NewScheduler() extends Scheduler

class SchedulerHook {

  var scheduler: Scheduler = FakeScheduler
  def setScheduler(s: Scheduler) =
    scheduler = s

  def run() {    
    setScheduler(NewScheduler())
    PrimaryNodeHook.expand() //expand replay
    PrimaryNodeHook.setup()
    scheduler.run()
  }

}
