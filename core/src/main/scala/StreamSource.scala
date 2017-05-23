package dawn.flow

class StreamSource[A](source: Source[Stream[A]]) {

  def reduceF(r: (A, A) => A) =
    source.map(
      (x: Stream[A]) => x.reduce(r), "Reduce " + r.toString)

}
