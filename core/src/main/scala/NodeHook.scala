package dawn.flow

import collection.mutable.Queue
import collection.mutable.Set
import com.github.mdr.ascii.graph._

trait NodeHook {

  var nodes = List[Node]()

  def addNode(s: Node) = {
//    println(s)
    nodes ::= s
  }

  def sinks = nodes.filter(x => x match {
    case s: Sink => true
    case _ => false
  })
  

  def getSources(n: Node) = n match {
    case x: Buffer[_] => List()
    case _ => n.sources
  }

  def toGraph(n: List[Node]) = {
    Graph(n.toSet, n.flatMap(x => getSources(x).map(y => (x, y))))
  }

  def drawGraph() =
    Node.drawGraph(sinks.toSeq)

  def expand() = {
    println(nodes.flatMap(_.sources).length) //Trick to access once sources
    println("Done expansion")
  }
  
  def setup() = {
    val sorted = GraphUtils.topologicalSort(toGraph(nodes)).get.reverse
    println(sorted)
    sorted.foreach(_.setup())
    println("Done setup")
  }


}
