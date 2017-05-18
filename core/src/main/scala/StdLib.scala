package dawn.flow


class StdLibSource[A, B](source: Source[A, B]) {

  def cache() = Cache(source)

  def buffer(init: A) = Buffer(() => source, init)
}

case class Clock(dt: Timestep) extends Source[Time, Null]  {
  override def toString = "Clock " + dt 
  def sources = List()
  def stream(p: Null) = genPerfectTimes(dt)
}

case class Buffer[A,B](source1: () =>Source[A,B], init: A) extends Op1[A, B, A]  {

  lazy val source: Source[A,B] = source1()
  override lazy val sources = List(source)  
  def stream(p: B) = init #:: source.stream(p)

}


case class Cache[A, B](source: Source[A, B]) extends Op1[A, B, A] with Resettable {
  var cStream: Option[Stream[A]] = None

  def reset() = {
    println("Cache reset")
    cStream = None
  }

  def stream(p: B) = {
    if (cStream.isEmpty) {
      cStream = Some(source.stream(p))
    }
    cStream.get
  }
}
