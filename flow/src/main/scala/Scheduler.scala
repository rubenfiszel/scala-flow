package dawn.flow

trait Scheduler {

  def schedule[A](sinkTs: Seq[Sink[A]], model: A): Unit

}


object SimpleScheduler extends Scheduler {

  def schedule[A](sinkTs: Seq[Sink[A]], model: A) =
    sinkTs.foreach(_.consumeAll(model))
}
/*
object LastScheduler extends Scheduler {

  def schedule[A](sinkTs: Seq[Sink[A]], model: A) = sinkTs.foreach {_ match {
//    case x: SinkT[A] => ???
    case x: Sink[A] => x.consumeAll(model)
  }}

  
}
*/
