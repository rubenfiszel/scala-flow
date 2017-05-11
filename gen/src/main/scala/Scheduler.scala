package spatial.fusion.gen

trait Scheduler {

  def schedule[A](sinkTs: Seq[Sink[_, A]]): Unit

}

object LastScheduler extends Scheduler {

  def schedule[A](sinkTs: Seq[Sink[_, A]]) = sinkTs match {
    case x: SinkT[_, A] => ???
    case x: Sink[_, A] => ???      
  }

//  def schedule(sinkTs: Seq[SinkT[_]]) = {
//    var m: Map[Time, 
//  }
  
}
