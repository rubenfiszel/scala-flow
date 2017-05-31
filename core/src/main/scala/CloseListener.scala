package dawn.flow

trait CloseListener extends Node { self =>
  def closePriority: Int = 0
  def schedulerClose: Scheduler
  def onScheduleClose(): Unit

  override def setup() = {
    super.setup()
    schedulerClose.addCloseListener(self)
  }
}
