package dawn.flow

import com.github.mdr.ascii.graph._
import scala.reflect._
import com.github.mdr.ascii.layout.GraphLayout

trait Sourcable { self =>
  def sources: Seq[Source[_]]
  def requireModel = RequireModel.isRequiring(self)
  override def toString = getClass.getSimpleName
}

object Sourcable {

  val model: Sourcable = new Sourcable {
    def sources = List()
    override def toString = "Model"
  }

  def addModel(x: List[Sourcable]) =
    x.filter(_.requireModel).map(s => (model, s))


  def graph(s: Seq[Sourcable],
    g: Graph[Sourcable] = Graph(Set(model), List())): Graph[Sourcable] = {
    val nvertices = g.vertices ++ s.toSet ++ s.flatMap(_.sources)
    val nedges = (g.edges ++ addModel(s.toList) ++ s.toList.flatMap(x => x.sources.map(y => (y, x)))).distinct
    val init = g.copy(
        vertices = nvertices,
        edges = nedges)
    s.foldLeft(init)((acc, pos) => graph(pos.sources.filter(!g.vertices.contains(_)), acc))
  }

  def drawGraph(s: Seq[Sourcable]) =
    println(GraphLayout.renderGraph(graph(s)))

  def collectResettable(s: Seq[Sourcable]) = {
    graph(s).vertices
      .filter {
        _ match {
          case a: Resettable =>
            true
          case _ =>
            false
        }
      }
      .map(_.asInstanceOf[Resettable])
  }

  def addParams(s: Sourcable): Set[Any] = s match {
    case x: Product => x.productIterator.toSet
    case _          => Set()
  }
}
