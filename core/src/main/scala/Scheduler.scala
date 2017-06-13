package dawn.flow

import collection.mutable.PriorityQueue

case class Event(t: Time, f: () => Unit)

case object FakeScheduler extends Scheduler

case class NewScheduler(i: Int) extends Scheduler

object Scheduler {
  var i = 0
  def newOne() = NewScheduler({ i += 1; i })
}

trait Scheduler {

  implicit val ordCl: Ordering[CloseListener] = Ordering.by {
    pc: CloseListener =>
      pc.closePriority
  }

  val closeListeners = PriorityQueue[CloseListener]()

  var childSchedulers = List[Scheduler]()

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

  def executeIn(f: => Unit, dt: Time = 0.0): Unit = {
    registerEvent(f, now + dt)
  }

  def run() = {
    while (!pq.isEmpty) {
      val dq = pq.dequeue
      now = dq.t
      dq.f()
    }
    closeListeners.dequeueAll.foreach(_.onScheduleClose())
  }

}
