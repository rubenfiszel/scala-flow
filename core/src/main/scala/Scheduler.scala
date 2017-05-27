package dawn.flow

import collection.mutable.PriorityQueue

case class Event(t: Time, f: () => Unit) 

trait CloseListener {
  self =>
  def shClose: Scheduler
  def close(): Unit
  shClose.addCloseListener(self)
}

trait Scheduler {

  var closeListeners = List[CloseListener]()

  def addCloseListener(cl: CloseListener) =
    closeListeners ::= cl

  implicit val ord: Ordering[Event] = Ordering.by { e: Event => -e.t }

  val pq = PriorityQueue[Event]()

//  def registerEvent(f: () => Unit, t: Time): Unit =
//    pq.enqueue(Event(t, f))

  def registerEvent(f: => Unit, t: Time): Unit = {
//    println("ENQUEUE" +t)
    pq.enqueue(Event(t, () => f))
  }

  
  def run() = {
    while (!pq.isEmpty) {
      val dq = pq.dequeue
      val f = dq.f
//      println("DEQEUE " + dq.t)      
      f()
    }
    closeListeners.foreach(_.close())
  }
}

trait EmitterStream[A] extends Source[A] with Source0 with Resettable {
  def stream(): Stream[(Time, A)]
  var iterator = stream().toIterator

  def registerNext(): Unit = {
    if (iterator.hasNext) {
      val (t, a) = iterator.next
      broadcast(a, t)
      sh.registerEvent(registerNext(), t)
    }
  }
  sh.registerEvent(registerNext(), -1)

  def reset() = {
    iterator = stream.toIterator
  }
}
