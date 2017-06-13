package dawn.flow

import collection.mutable.Queue

trait Source[A] extends Node { parent =>

  var closed = false

  private var channels = List[Channel[A]]()

  def name: String

  def close() {
    closed = true
  }

  override def reset() = {
    super.reset()
    channels = List()
    closed = false
  }

  def addChannel(c: Channel[A]) =
    channels ::= c

  def broadcast(x: => Timestamped[A], t: Time = 0) =
    if (!closed)
      channels.foreach(_.push(x, t))

  override def toString = name

  private def lift[B, C](f: B => C): Timestamped[B] => C =
    new NamedFunction((x: Timestamped[B]) => f(x.v),
                      f.toString,
                      RequireModel.isRequiring(f))

  private def liftBool[B](b: B => Boolean): Timestamped[B] => Boolean =
    new NamedFunction((x: Timestamped[B]) => b(x.v),
                      b.toString,
                      RequireModel.isRequiring(b))

  private def liftUnit[B](f: B => Unit): Timestamped[B] => Unit =
    new NamedFunction((x: Timestamped[B]) => f(x.v),
                      f.toString,
                      RequireModel.isRequiring(f))

  private def liftList[B, C](
      f: B => List[C]): Timestamped[B] => List[Timestamped[C]] =
    new NamedFunction(
      (x: Timestamped[B]) => f(x.v).map(y => Timestamped(x.t, y, x.dt)),
      f.toString,
      RequireModel.isRequiring(f))

  def filterT(b: Timestamped[A] => Boolean, name1: String = ""): Source[A] =
    new Op1[A, A] {
      def rawSource1 = parent
      def listen1(x: Timestamped[A]) = {
        if (b(x))
          broadcast(x)
      }

      def name = "Filter " + getStrOrElse(name1, b.toString)
      override def requireModel = RequireModel.isRequiring(b)
    }

  def filter(b: A => Boolean, name1: String = ""): Source[A] =
    filterT(liftBool(b), name1)

  def foreachT(f: Timestamped[A] => Unit, name1: String = ""): Source[A] =
    new Op1[A, A] {
      def rawSource1 = parent
      def listen1(x: Timestamped[A]) = {
        f(x)
        broadcast(x)
      }

      def name = "Foreach " + getStrOrElse(name1, f.toString)
      override def requireModel = RequireModel.isRequiring(f)
    }

  def debug(): Source[A] =
    foreachT(x => Console.println(s"[$this]: $x)"), "Debug")

  def foreach(f: A => Unit, name1: String = ""): Source[A] =
    foreachT(liftUnit(f), name1)

  //USE ONLY WHEN THERE IS NO LOOP INVOLVED
  def buffer(init: A) =
    Buffer(this, init, this)

  //Get old A but with the time of the new A
  def bufferWithTime(init: A) =
    zip(buffer(init))
      .map(x => x._2)

  def takeWhileT(b: Timestamped[A] => Boolean, name1: String = ""): Source[A] =
    new Op1[A, A] {
      def rawSource1 = parent
      def listen1(x: Timestamped[A]) = {
        if (b(x))
          broadcast(x)
        else {
          parent.closed = true
          closed = true
        }
      }

      def name = "TakeWhile " + getStrOrElse(name1, b.toString)
      override def requireModel = RequireModel.isRequiring(b)
    }

  def takeWhile(b: A => Boolean, name1: String = ""): Source[A] =
    takeWhileT(liftBool(b), name1)

  def mapT[B](f: Timestamped[A] => B,
              name1: String = ""): Source[B] =
    new Op1[A, B] {
      def rawSource1 = parent
      def listen1(x: Timestamped[A]) = {
        val mx = Timestamped(x.t, f(x), x.dt)
        broadcast(mx)
      }

      def name = "Map " + getStrOrElse(name1, f.toString)
      override def requireModel = RequireModel.isRequiring(f)
    }

  def map[B](f: A => B, name1: String = ""): Source[B] =
    mapT(lift(f), name1)

  def flatMapT[C](f: Timestamped[A] => List[Timestamped[C]],
                  name1: String = ""): Source[C] =
    new Op1[A, C] {
      def rawSource1 = parent
      def listen1(x: Timestamped[A]) = {
        f(x).foreach(x => broadcast(x))
      }

      def name = "FlatMap " + getStrOrElse(name1, f.toString)
      override def requireModel = RequireModel.isRequiring(f)
    }

  def flatMap[C](f: A => List[C], name1: String = ""): Source[C] =
    flatMapT(liftList(f), name1)

  //Divide the frequency of the stream by n
  def divider(n: Int) =
    new Op1[A, A] {
      def rawSource1 = parent
      var i = 0
      def listen1(x: Timestamped[A]) = {
        i += 1
        if (i % n == 0)
          broadcast(x)
      }
      def name = "Divider " + n
    }

  private def to2TS[B, C](x1: Timestamped[B], x2: Timestamped[C]) = {
//    val t  = Math.max(x1.t, x2.t)
//    val dt = Math.max(x1.dt, x2.dt)
    val t = x1.t
    val dt = x1.dt
    Timestamped(t, (x1, x2), dt)
  }

  //Zip everthing
  // A1 A2 B1 A3 B2 B3 B4 B5 A4=> (A1, B1), (A2, B2), (A3, B3), (A4, B4), [Queue[B5]]
  def zipT[B](s2: Source[B]) =
    new Op2[A, B, (Timestamped[A], Timestamped[B])] {
      def rawSource1 = parent
      def rawSource2 = s2
      val q1: Queue[Timestamped[A]] = Queue()
      val q2: Queue[Timestamped[B]] = Queue()
      def listen1(x: Timestamped[A]) = {
        if (q2.isEmpty)
          q1.enqueue(x)
        else {
          val dq = q2.dequeue()
          broadcast(to2TS(x, dq))
        }
      }

      def listen2(x: Timestamped[B]) = {
        if (q1.isEmpty)
          q2.enqueue(x)
        else {
          val dq = q1.dequeue()
          broadcast(to2TS(dq, x))
        }
      }

      def name = "Zip2"
    }

  def unzip2[B, C](implicit ev: A <:< (B, C)): (Source[B], Source[C]) = {
    val s = map(ev.apply, "Unzip")
    (s.map(_._1), s.map(_._2))
  }

  private def mergeTS[B, C](
      x: Timestamped[(Timestamped[B], Timestamped[C])]): (B, C) =
    (x.v._1.v, x.v._2.v)

  def zip[B](s2: Source[B]) =
    zipT(s2).mapT(mergeTS, "ZipT")

  //Only zip the last of the right
  // A1 A2 B1 A3 B2 B3 B4 B5 A4=> (A1, B1), (A2, B2), (A3, B3), (A4, B5)
  def zipLastRightT[B](s2: Source[B]): Source[(Timestamped[A], Timestamped[B])] =
    new Op2[A, B, (Timestamped[A], Timestamped[B])] {
      def rawSource1 = parent
      def rawSource2 = s2
      val q1: Queue[Timestamped[A]] = Queue()
      var lastB: Option[Timestamped[B]] = None

      def listen1(x: Timestamped[A]) = {
        if (lastB.isEmpty)
          q1.enqueue(x)
        else {
          val lB = lastB.get
          lastB = None
          broadcast(to2TS(x, lB))
        }
      }

      def listen2(x: Timestamped[B]) = {
        if (q1.isEmpty)
          lastB = Some(x)
        else {
          val dq = q1.dequeue()
          lastB = None          
          broadcast(to2TS(dq, x))
        }
      }

      def name = "ZipLastRight"
    }

  def zipLastRight[B](s2: Source[B]) =
    zipLastRightT(s2).mapT(mergeTS, "ZipLastRightT")


  //Only zip the last pair
  // A1 A2 B1 A3 B2 B3 A4=> (A1, B1), (A3, B2), (B3, A4)
  def zipLastT[B](s2: Source[B]): Source[(Timestamped[A], Timestamped[B])] =
    new Op2[A, B, (Timestamped[A], Timestamped[B])] {
      def rawSource1 = parent
      def rawSource2 = s2
      var lastA: Option[Timestamped[A]] = None      
      var lastB: Option[Timestamped[B]] = None

      def listen1(x: Timestamped[A]) = {
        if (lastB.isEmpty)
          lastA = Some(x)
        else {
          val lB = lastB.get
          lastB = None
          broadcast(to2TS(x, lB))
        }
      }

      def listen2(x: Timestamped[B]) = {
        if (lastA.isEmpty)
          lastB = Some(x)
        else {
          val lA = lastA.get
          lastA = None          
          broadcast(to2TS(lA, x))
        }
      }

      def name = "ZipLast"
    }

  def zipLast[B](s2: Source[B]) =
    zipLastT(s2).mapT(mergeTS, "ZipLastT")
  
  def merge[B](s2: Source[B]): Source[Either[A, B]] =
    new Op2[A, B, Either[A, B]] {
      def rawSource1 = parent
      def rawSource2 = s2
      def listen1(x: Timestamped[A]) =
        broadcast(x.map(Left(_)))

      def listen2(x: Timestamped[B]) =
        broadcast(x.map(Right(_)))

      def name = "Merge"
    }

  def fusion(sources: Source[A]*): Source[A] = {
    def flatten(x: Either[A, A]): A =
      x match {
        case Left(y) => y
        case Right(y) => y
      }
    sources.foldLeft(parent)((acc, pos) =>
      acc.merge(pos).map(flatten, "Fusion"))
  }

  def latency(dt: Time) =
    new Op1[A, A] {
      def rawSource1 = parent
      def listen1(x: Timestamped[A]) = {
        broadcast(x.addLatency(dt), dt)
      }
      def name = "LatencyT " + dt
    }

  def toTime =
    mapT(_.t)

  def drop(n: Int) = {
    new Op1[A, A] {
      def rawSource1 = parent
      var i = n
      def listen1(x: Timestamped[A]) = {
        if (i == 0)
          broadcast(x)
        else
          i -= 1
      }
      def name = "Drop " + n
    }

  }

}
