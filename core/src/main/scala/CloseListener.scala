package dawn.flow

trait CloseListener { self =>
  def closePriority: Int = 0
  def schedulerClose: Scheduler
  def onScheduleClose(): Unit
  schedulerClose.addCloseListener(self)
}
