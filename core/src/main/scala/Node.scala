package dawn.flow

import com.github.mdr.ascii.graph._
import com.github.mdr.ascii.layout.GraphLayout

trait Node { self =>
  def nodeHook: NodeHook
  nodeHook.addNode(self)
  def scheduler: Scheduler
  def rawSources: Seq[Node]
  def sources: Seq[Node]
  def requireModel = RequireModel.isRequiring(self)
  override def toString = getClass.getSimpleName
  def setup(): Unit = ()
}

object Node {

  val model: Node = new Node {
    def scheduler = ???
    def nodeHook = new NodeHook {}
    def sources = List()
    def rawSources = List()
    override def toString = "Model"
  }

  def addModel(x: List[Node]) =
    x.filter(_.requireModel).map(s => (model, s))

  def graph(s: Seq[Node],
            g: Graph[Node] = Graph(Set(model), List())): Graph[Node] = {
    val nvertices = g.vertices ++ s.toSet ++ s.flatMap(_.rawSources)
    val nedges = (g.edges ++ addModel(s.toList) ++ s.toList.flatMap(x =>
      x.rawSources.map(y => (y, x)))).distinct
    val init = g.copy(vertices = nvertices, edges = nedges)
    s.foldLeft(init)((acc, pos) =>
      graph(pos.rawSources.filter(!g.vertices.contains(_)), acc))
  }

  def drawGraph(s: Seq[Node]) =
    println(GraphLayout.renderGraph(graph(s)))

  def addParams(s: Node): Set[Any] = s match {
    case x: Product => x.productIterator.toSet
    case _ => Set()
  }
}
