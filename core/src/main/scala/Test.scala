package dawn.flow

import breeze.linalg._
import breeze.interpolation._

case class TestTS[A: Data](source1: SourceT[A],
                              source2: SourceT[A],
                              nb: Int)
    extends Sink
    with Source2T[A, A] {

  def toInterpolation(l: Array[Timestamped[A]]) = {
    val toV   = implicitly[Data[A]]
    val ts_x  = DenseVector(l.map(_.t))
    val min   = ts_x.min
    val max   = ts_x.max
    val ts_yt = l.map(x => toV.toValues(x.v))
    val ts_ys =
      (0 until ts_yt(0).length).map(i => DenseVector(ts_yt.map(_(i))))
    val interpolated = ts_ys.map(ys => CubicInterpolator(ts_x, ys))
    (interpolated, min, max)
  }

  def consumeAll() = {

    val datasA = source1.stream().toArray
    val truthA = source2.stream().toArray

    val (datas, minD, maxD) = toInterpolation(datasA)
    val (truth, minT, maxT) = toInterpolation(truthA)

    val minG = max(minD, maxT)
    val maxG = min(maxD, maxT)

    val tf = maxG - minG

    val lspace = linspace(minG, maxG, nb)
    val errs =
      datas.zip(truth).map(y => lspace.map(i => Math.abs(y._1(i) - y._2(i))))
    val sums = errs.map(_.sum)
    val sumSq = errs.map(_.map(x => x*x).sum)
    val maxs = errs.map(_.max)
    val avgs = sums.map(_ / nb)
    val rmse = sumSq.map(_ / nb)

    val rmseS = rmse.map(e => f"$e%e").mkString(", ")    
    val avgsS = avgs.map(e => f"$e%e").mkString(", ")
    val maxsS = maxs.map(e => f"$e%e").mkString(", ")

    println(s"[${Console.GREEN}flow info${Console.RESET}] RMSE       : $rmseS")    
    println(s"[${Console.GREEN}flow info${Console.RESET}] Mean errors: $avgsS")
    println(s"[${Console.GREEN}flow info${Console.RESET}] Max  errors: $maxsS")

  }
}