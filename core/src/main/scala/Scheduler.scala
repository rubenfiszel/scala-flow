package dawn.flow

trait Scheduler {

  def schedule(sinkTs: Seq[Sink]): Unit

}


object SimpleScheduler extends Scheduler {

  def schedule(sinkTs: Seq[Sink]) =
    sinkTs.foreach(_.consumeAll())
}
/*
object LastScheduler extends Scheduler {

  def schedule[A](sinkTs: Seq[Sink[A]], model: A) = sinkTs.foreach {_ match {
//    case x: SinkT[A] => ???
    case x: Sink[A] => x.consumeAll(model)
  }}

  
}
*/
