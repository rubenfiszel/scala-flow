package dawn.flow

class SchedulerHook {

  //A fake scheduler is enough to generate replay nodes correctly
  //Indeed, it acts as if all emitters had the same scheduler which
  //is enough information
  var scheduler: Scheduler = FakeScheduler
  def setScheduler(s: Scheduler) =
    scheduler = s

  def run() = {
    setScheduler(Scheduler.newOne())
    PrimaryNodeHook.setup()
    scheduler.run()
  }

}
