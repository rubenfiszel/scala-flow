package dawn.flow

import spire.math.{Real => _, _ => _}
import spire.implicits._
import spire.algebra.Field
import breeze.linalg.{norm, normalize, cross}

//https://robotics.stackexchange.com/questions/1717/how-to-determine-the-parameter-of-a-complementary-filter
case class ComplementaryFilter[A: Vec, B](source1: Source[A, B],
                                          source2: Source[A, B],
                                          init: A,
                                          alpha: Real)
    extends Block2[A, B, A, A] {

  lazy val lpf        = LowPassFilter(source1, init, alpha)
  lazy val hpf        = HighPassFilter(source2, init, alpha)
  lazy val zip        = lpf.zip(hpf)
  lazy val r          = zip.map(x => x._1 * alpha + x._2 * (1 - alpha))
  def genStream(p: B) = r.stream(p)

}

case class LowPassFilter[A: Vec, B](source: Source[A, B], init: A, alpha: Real)
    extends Block1[A, B, A] {
  val bYnm = Buffer(this, init)

  //y[n]=(1-alpha).x[n]+alpha.y[n-1]
  def f(xnYnm: (A, A)) = {
    val (xn, ynm) = xnYnm
    xn * (1 - alpha) + ynm * alpha
  }

  def genStream(p: B): Stream[A] = source.zip(bYnm).map(f _).stream(p)

}

case class HighPassFilter[A: Vec, B](source1: Source[A, B],
                                     init: A,
                                     alpha: Real)
    extends Block1[A, B, A] {

  lazy val common: Block1[(A, A), B, A] = new Block1[(A, A), B, A] {

    def source = source1
    val bYnm   = Buffer(common.map(_._1), init)
    val bXnm   = Buffer(common.map(_._2), init)
    //y[n]=(1-alpha)y[n-1]+(1-alpha)(x[n]-x[n-1])
    def f(xnXnmYnm: (A, (A, A))) = {
      val (xn, (xnm, ynm)) = xnXnmYnm
      val yn               = ynm * (1 - alpha) + (xn - xnm) * alpha
      (xn, yn)
    }
    def genStream(p: B): Stream[(A, A)] =
      source.zip(bXnm.zip(bYnm)).map(f _).stream(p)

  }

  def source                     = source1
  def genStream(p: B): Stream[A] = common.map(_._2).stream(p)

}
