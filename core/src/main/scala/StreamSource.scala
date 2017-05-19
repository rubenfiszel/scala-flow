package dawn.flow

class StreamSource[A, B](source: Source[Stream[A], B]) {

  def reduceF(r: (A, A) => A) =
    source.map(
      (x: Stream[A]) => x.reduce(r), "Reduce " + r.toString)

  def reduceF(r: (B, A, A) => A) =
    source.map(
      (p: B, s: Stream[A]) =>
                       s.reduce(Function.uncurried(r.curried(p))),
                     "Reduce2 " + r.toString)
}
