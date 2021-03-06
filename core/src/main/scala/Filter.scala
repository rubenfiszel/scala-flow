package dawn.flow

import spire.math.{Real => _, _ => _}
import spire.implicits._
import spire.algebra.Field
import breeze.linalg.{norm, normalize, cross}

case class ComplementaryFilter[A: Vec](rawSource1: Source[A],
                                       rawSource2: Source[A],
                                       init: A,
                                       alpha: Real)
    extends Block2[A, A, A] {

  def name = "ComplFilter"
  lazy val lpf = LowPassFilter(source1, init, alpha)
  lazy val hpf = HighPassFilter(source2, init, (1 - alpha))
  lazy val zip = lpf.zip(hpf)
  lazy val out = zip.map(x => x._1 * alpha + x._2 * (1 - alpha))

}

//https://en.wikipedia.org/wiki/Low-pass_filter
case class LowPassFilter[A: Vec](rawSource1: Source[A], init: A, alpha: Real)
    extends Block1[A, A] {

  def name = "LPF"

  //y[i] := y[i-1] + α * (x[i] - y[i-1])
  def f(xnYnm: (A, A)) = {
    val (xn, ynm) = xnYnm
    (xn - ynm) * alpha + ynm
  }

  lazy val bYnm: Source[A] = Buffer(out, init, source1)
  lazy val out = source1.zip(bYnm).map(f _)

}

case class HighPassFilter[A: Vec](rawSource1: Source[A], init: A, alpha: Real)
    extends Block1[A, A] {

  def name = "HPF"
  def f(xnXnmYnm: (A, (A, A))) = {
    val (xn, (xnm, ynm)) = xnXnmYnm
    val yn = (xn + ynm - xnm) * alpha
    (xn, yn)
  }

  lazy val common =
    source.zip(bXnmYnm).map(f _)

  val bXnmYnm: Source[(A, A)] = Buffer(common, (init, init), source1)

  def source = source1

  val out = common.map(_._2)

}
