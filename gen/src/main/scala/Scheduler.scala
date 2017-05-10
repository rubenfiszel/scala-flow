package spatial.fusion.gen

trait Scheduler {

  def schedule(sinkTs: Seq[Sink[_]]): Unit

}

case class A()(implicit m: Int) 
object LastScheduler extends Scheduler {

  def schedule(sinkTs: Seq[Sink[_]]) = sinkTs match {
    case x: SinkT[_] => ???
    case x: Sink[_] => ???      
  }

//  def schedule(sinkTs: Seq[SinkT[_]]) = {
//    var m: Map[Time, 
//  }
  
}
