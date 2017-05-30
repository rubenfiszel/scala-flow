package dawn.flow

import collection.mutable.PriorityQueue

case class Event(t: Time, f: () => Unit)

trait CloseListener { self =>
  def closePriority: Int = 0
  def schedulerClose: Scheduler
  def close(): Unit
  schedulerClose.addCloseListener(self)
}

object Scheduler {
  val BEFORE_START = -3
  val AT_START     = 0
}

case class SecondaryScheduler() extends Scheduler

trait Scheduler {

  implicit val ordCl: Ordering[CloseListener] = Ordering.by {
    pc: CloseListener =>
      pc.closePriority
  }
  val closeListeners = PriorityQueue[CloseListener]()

  def addCloseListener(cl: CloseListener) =
    closeListeners.enqueue(cl)

  implicit val ordE: Ordering[Event] = Ordering.by { e: Event =>
    -e.t
  }

  val pq = PriorityQueue[Event]()

  var now =
    0.0

  def registerEvent(f: => Unit, t: Time): Unit = {
    pq.enqueue(Event(t, () => f))
  }

  def executeBeforeStart(f: => Unit): Unit = {
    registerEvent(f, Scheduler.BEFORE_START)
  }

  def executeAtStart(f: => Unit): Unit = {
    registerEvent(f, Scheduler.AT_START)
  }

  def executeIn(f: => Unit, dt: Time = 0.0): Unit = {
    registerEvent(f, now + dt)
  }

  def run() = {
    while (!pq.isEmpty) {
      val dq = pq.dequeue
      now = dq.t
      dq.f()
    }
    closeListeners.dequeueAll.foreach(_.close())
  }

}

