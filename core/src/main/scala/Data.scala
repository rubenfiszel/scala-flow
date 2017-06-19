package dawn.flow

class DataSource[A: Data](source: Source[A]) {

  def labelData(f: Time => A): Source[LabeledData[A]] =
    source.mapT(x => new LabeledData[A] {
      def data = implicitly[Data[A]]
      def value = x.v
      def label = f(x.t)
    }, "LabelData")

}

trait Data[A] {
  def toVector(x: A): VectorR
}


trait LabeledData[A] {
  def data: Data[A]
  def value: A
  def label: A
}
