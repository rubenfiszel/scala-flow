package dawn.flow

import collection.mutable.Queue

trait Source[A] extends Sourcable {
  parent =>
  private var channels = List[Channel[A]]()

  def sh: Scheduler

  def name: String

  def addChannel(c: Channel[A]) = 
    channels ::= c
  

  def broadcast(x: => A, t: Time = -1) =
    channels.foreach(_.push(x, t))

  override def toString = name

  def filter(b: (A) => Boolean, name1: String = ""): Source[A] =
    new Op1[A, A] {
      def source1 = parent
      def listen1(x: => A) =
        if (b(x))
          broadcast(x)

      def name                  = "Filter " + getStrOrElse(name1, b.toString)
      override def requireModel = RequireModel.isRequiring(b)
    }

  def takeWhile(b: (A) => Boolean, name1: String = ""): Source[A] =
    new Op1[A, A] {
      def source1 = parent
      def listen1(x: => A) = {
        if (b(x))
          broadcast(x)
      }

      def name                  = "takeWhile " + getStrOrElse(name1, b.toString)
      override def requireModel = RequireModel.isRequiring(b)
    }

  def map[C](f: (A) => C, name1: String = ""): Source[C] =
    new Op1[A, C] {
      def source1 = parent
      def listen1(x: => A) = {
        broadcast(f(x))
      }

      def name                  = "Map " + getStrOrElse(name1, f.toString)
      override def requireModel = RequireModel.isRequiring(f)
    }

  def flatMap[C](f: (A) => List[C], name1: String = ""): Source[C] =
    new Op1[A, C] {
      def source1 = parent
      def listen1(x: => A) = {
        f(x).foreach(x => broadcast(x))
      }

      def name                  = "FlatMap " + getStrOrElse(name1, f.toString)
      override def requireModel = RequireModel.isRequiring(f)
    }

  
  //Divide the frequency of the stream by n
  def divider(n: Int) =
    new Op1[A, A] {
      def source1 = parent
      var i       = 0
      def listen1(x: => A) = {
        i += 1
        if (i % n == 0)
          broadcast(x)
      }
      def name = "Divider " + n
    }

  def zip[B](s2: Source[B]) =
    new Op2[A, B, (A, B)] {
      def source1            = parent
      def source2            = s2
      val q1: Queue[() => A] = Queue()
      val q2: Queue[() => B] = Queue()
      def listen1(x: => A) = {
        if (q2.isEmpty)
          q1.enqueue(() => x)
        else {
          val dq = q2.dequeue()
          broadcast((x, dq()))
        }
      }

      def listen2(x: => B) = {
        if (q1.isEmpty)
          q2.enqueue(() => x)
        else {
          val dq = q1.dequeue()          
          broadcast((dq(), x))
        }
      }

      def name = "Zip2"
    }

  def zipLast[B](s2: Source[B]): Source[(A, B)] =
        new Op2[A, B, (A, B)] {
      def source1            = parent
      def source2            = s2
      val q1: Queue[() => A] = Queue()
      var lastB: Option[() => B] = None
      def listen1(x: => A) = {
        if (lastB.isEmpty)
          q1.enqueue(() => x)
        else {
          val lB = lastB.get
          lastB = None
          broadcast((x, lB()))
        }
      }

      def listen2(x: => B) = {
        if (q1.isEmpty)
          lastB = Some(() => x)
        else {
          val dq = q1.dequeue()                    
          broadcast((dq(), x))
          lastB = None
        }
      }

      def name = "ZipLast"
    }

  
  def merge[B](s2: Source[B]): Source[Either[A, B]] =
    new Op2[A, B, Either[A, B]] {
      def source1 = parent
      def source2 = s2
      def listen1(x: => A) =
        broadcast(Left(x))

      def listen2(x: => B) =
        broadcast(Right(x))

      def name = "Merge"
    }

  def fusion(sources: Source[A]*): Source[A] = {
    def flatten(x: Either[A, A]): A =
      x match {
        case Left(y)  => y
        case Right(y) => y
      }
    sources.foldLeft(parent)((acc, pos) =>
      acc.merge(pos).map(flatten _, "Fusion"))
  }

}

trait Source0 extends Sourcable {
  lazy val sources: List[Source[_]] = List()
}

trait Source1[A] extends Sourcable {
  self => 
  def sh: Scheduler = source1.sh
  def source1: Source[A]
  lazy val sources: List[Source[_]] = List(source1)
  def listen1(x: => A)
  sh.registerEvent(source1.addChannel(Channel1(self, sh)), -2)
}

trait Source2[A, B] extends Source1[A] {
  self =>
  def source2: Source[B]
  override lazy val sources: List[Source[_]] = List(source1, source2)
  def listen2(x: => B)
  sh.registerEvent(source2.addChannel(Channel2(self, sh)), -2)  
//  source2.addChannel(Channel2(this, sh))    
}

trait Source3[A, B, C] extends Source2[A, B] {
  self =>
  def source3: Source[C]
  override lazy val sources: List[Source[_]] = List(source1, source2, source3)
  def listen3(x: => C)
  sh.registerEvent(source3.addChannel(Channel3(self, sh)), -2)    
}

trait Source4[A, B, C, D] extends Source3[A, B, C] {
  self =>
  def source4: Source[D]
  override lazy val sources: List[Source[_]] =
    List(source1, source2, source3, source4)
  def listen4(x: => D)
  sh.registerEvent(source4.addChannel(Channel4(self, sh)), -2)      
}

trait Block[A] {
  parent: Source[A] =>
  def out: Source[A]

  new Op1[A, A] {
    def source1 = out
    def listen1(x: => A) = 
      parent.broadcast(x)
    def name = "Op Block"
  }
}

trait Block1[A, B]       extends Source[B] with Source1[A] with Block[B] {
  def listen1(x: => A) = ()
}
trait Block2[A, B, C]    extends Source[C] with Source2[A, B] with Block[C] {
  def listen1(x: => A) = ()  
  def listen2(x: => B) = ()
}
trait Block3[A, B, C, D] extends Source[D] with Source3[A, B, C] with Block[D] {
  def listen1(x: => A) = ()
  def listen2(x: => B) = ()  
  def listen3(x: => C) = ()  
}
trait Block4[A, B, C, D, E]
    extends Source[E]
    with Source4[A, B, C, D]
    with Block[E] {
  def listen1(x: => A) = ()
  def listen2(x: => B) = ()
  def listen3(x: => C) = ()    
  def listen4(x: => D) = ()
}

trait Op1[A, B] extends Source[B] with Source1[A]
trait Op2[A, B, C] extends Source[C] with Source2[A, B]
trait Op3[A, B, C, D] extends Source[D] with Source3[A, B, C] 
trait Op4[A, B, C, D, E] extends Source[E] with Source4[A, B, C, D]

sealed trait Channel[A] {
  def push(x: => A, t: Time): Unit
}

case class Channel1[A](receiver: Source1[A], sh: Scheduler)
    extends Channel[A] {
  def push(x: => A, t: Time) = sh.registerEvent(receiver.listen1(x), t)
}

case class Channel2[A](receiver: Source2[_, A], sh: Scheduler)
    extends Channel[A] {
  def push(x: => A, t: Time) = sh.registerEvent(receiver.listen2(x), t)
}

case class Channel3[A](receiver: Source3[_, _, A], sh: Scheduler)
    extends Channel[A] {
  def push(x: => A, t: Time) = sh.registerEvent(receiver.listen3(x), t)
}

case class Channel4[A](receiver: Source4[_, _, _, A], sh: Scheduler)
    extends Channel[A] {
  def push(x: => A, t: Time) = sh.registerEvent(receiver.listen4(x), t)
}

trait Resettable {
  def reset(): Unit
}

abstract class NamedFunction[A, B](f: A => B, name: String) extends (A => B) {
  override def toString = name
  def apply(x: A)       = f(x)
}
