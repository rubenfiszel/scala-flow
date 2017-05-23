package dawn.flow

import spire.math.{Real => _, _ => _}
import spire.implicits._
import spire.algebra.Field
import breeze.linalg.{norm, normalize, cross}

case class ComplementaryFilter[A: Vec, B](source1: Source[A, B],
                                          source2: Source[A, B],
                                          init: A,
                                          alpha: Real)
    extends Op2[A, B, A, A] {

  lazy val lpf        = LowPassFilter(source1, init, alpha)
  lazy val hpf        = HighPassFilter(source2, init, (1 - alpha))
  lazy val zip        = lpf.zip(hpf)
  lazy val r          = zip.map(x => x._1 * alpha + x._2 * (1 - alpha))
  def genStream(p: B) = r.stream(p)

}

//https://en.wikipedia.org/wiki/Low-pass_filter
case class LowPassFilter[A: Vec, B](source: Source[A, B], init: A, alpha: Real)
    extends Op1[A, B, A] {
  lazy val bYnm: Source[A, B] = Buffer(out, init)

  //y[i] := y[i-1] + α * (x[i] - y[i-1])
  def f(xnYnm: (A, A)) = {
    val (xn, ynm) = xnYnm
    (xn - ynm) * alpha + ynm
  }

  lazy val out                   = source.zip(bYnm).map(f _)
  def genStream(p: B): Stream[A] = out.stream(p)

}

case class HighPassFilter[A: Vec, B](source1: Source[A, B],
                                     init: A,
                                     alpha: Real)
    extends Op1[A, B, A] {

  def f(xnXnmYnm: (A, (A, A))) = {
    val (xn, (xnm, ynm)) = xnXnmYnm
    val yn               = (xn + ynm - xnm) * alpha
    (xn, yn)
  }

  lazy val common =
    source.zip(bXnmYnm).map(f _)

  val bXnmYnm: Source[(A, A), B] = Buffer(common, (init, init))

  def source                     = source1
  def genStream(p: B): Stream[A] = common.map(_._2).stream(p)

}
