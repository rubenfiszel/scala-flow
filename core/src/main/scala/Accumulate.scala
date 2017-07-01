package dawn.flow

trait Accumulate[A] extends SourceN {
  var accumulated: Array[ListT[A]] = _
  override def listenN(i: Int, x: Timestamped[_]) = {
    accumulated(i-1) ::= x.asInstanceOf[Timestamped[A]]
  }

  override def setup() = {
    super.setup()
    accumulated = Array.fill(rawSources.length)(List[Timestamped[A]]())
  }
}

