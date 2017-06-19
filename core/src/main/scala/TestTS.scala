package dawn.flow

import breeze.linalg._

class TestTS[A: Data](val rawSource1: Source[LabeledData[A]], tlog: Option[TestLogger]) extends SinkBatch1[LabeledData[A]] {

  def consumeAll(l: ListT[LabeledData[A]]) = {

    def data = implicitly[Data[A]]
    val toV = l.map(x => (data.toVector(x.v.value), data.toVector(x.v.label)))
    val errs = (0 until toV(0)._1.length).map(y => toV.map(x => Math.abs(x._1(y) - x._2(y))))
    val sums  = errs.map(_.sum)
    val sumSq = errs.map(_.map(x => x * x).sum)
    val maxs  = errs.map(_.max)
    val avgs  = sums.map(_ / l.length)
    val rmse  = sumSq.map(_ / l.length)

    val rmseS = rmse.map(e => f"$e%e").mkString(", ")
    val avgsS = avgs.map(e => f"$e%e").mkString(", ")
    val maxsS = maxs.map(e => f"$e%e").mkString(", ")

    tlog.foreach(_.addRMSE(rmse))

    println(s"[${Console.GREEN}flow info${Console.RESET}] RMSE       : $rmseS")
    println(s"[${Console.GREEN}flow info${Console.RESET}] Mean errors: $avgsS")
    println(s"[${Console.GREEN}flow info${Console.RESET}] Max  errors: $maxsS")

  }
}

object TestTS {
  def apply[A: Data](s: Source[LabeledData[A]], tlog: Option[TestLogger] = None) =
    new TestTS(s, tlog)
}

class TestLogger {
  var log = List[Seq[Real]]()

  def printAll() = {
    println("TestLogger printAll")
    log.foreach(x => println(x.map(e => f"$e%e").mkString(" ")))
  }

  def printMean() = {
    var r = Array.fill(log(0).size)(0.0)
    log.foreach(_.zipWithIndex.foreach { case (x, i) => r(i) += x } )
    r = r.map(_/log.length)
    println("TestLogger printMean")    
    println(r.map(e => f"$e%e").mkString(" "))
  }

  def addRMSE(x: Seq[Real]) =
    log ::= x
}
