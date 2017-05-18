package dawn.flow

import spire.math.{Real => _, _ => _}
import spire.implicits._
import spire.algebra.Field
import breeze.linalg.{norm, normalize, cross}


//https://robotics.stackexchange.com/questions/1717/how-to-determine-the-parameter-of-a-complementary-filter
case class ComplementaryFilter[A: Field,B](source1: Source[A, B], source2: Source[A, B], init: A, alpha: Real)
    extends Block2[A, B, A, A] {

  val lpf = LowPassFilter(source1, init, alpha)
  val hpf = HighPassFilter(source2, init, alpha)
  val zip = lpf.zip(hpf)
  val r = zip.map(x => alpha*x._1 + (1-alpha)*x._2)
  def stream(p: B) = r.stream(p)
}

case class LowPassFilter[A: Field, B](source: Source[A,B], init: A, alpha: Real) extends Op1[A, B, A] {
  var ynm: A = init

  //y[n]=(1-alpha).x[n]+alpha.y[n-1]
  def f(xn: A) = {
    val yn = (1-alpha)*xn + alpha*ynm
    ynm = yn
    yn
  }

  def stream(p: B): Stream[A] = source.stream(p).map(f)

}

case class HighPassFilter[A: Field, B](source: Source[A,B], init: A, alpha: Real) extends Op1[A, B, A] {

  var xnm: A = init
  var ynm: A = init  

  //y[n]=(1-alpha)y[n-1]+(1-alpha)(x[n]-x[n-1])  
  def f(xn: A) = {
    val yn = (1-alpha)*ynm + alpha*(xn-xnm)
    xnm = xn
    ynm = yn
    yn
  }

  def stream(p: B): Stream[A] = source.stream(p).map(f)

}
