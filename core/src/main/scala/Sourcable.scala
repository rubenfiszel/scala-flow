package dawn.flow

import com.github.mdr.ascii.graph._

trait Sourcable { self =>
  def sources: Seq[Source[_, _]]

  def reset(): Unit = {
    sources.foreach(_.resetCache())
    sources.foreach(_.reset())
  }

  override def toString = getClass.getSimpleName
}

object Sourcable {

  def graph(s: Seq[Sourcable], g: Graph[Any] = Graph(Set(), List())): Graph[Any] = {
    s.foldLeft(g.copy(
        vertices = g.vertices ++ s.toSet ++ s.flatMap(_.sources) ++ s.flatMap(addParams).toSet,
        edges  = g.edges ++ s.toList.flatMap(x => x.sources.map(y => (y, x)))
    ))((acc, pos) => graph(pos.sources, acc))
  }

  def addParams(s: Sourcable): Set[Any] = s match {
    case x: Product => x.productIterator.toSet
    case _ => Set()
  }
}
