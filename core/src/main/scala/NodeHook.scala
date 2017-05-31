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

  //Expand replay blocks
  def expand() = {
    //Trick to access once sources and generate Replay op
    val length = nodes.flatMap(_.sources).length
    println("Done expansion " + length + " blocks" )
  }
  
  def setup() = {
    expand()
    val sorted = GraphUtils.topologicalSort(toGraph(nodes)).get.reverse
    sorted.foreach(_.setup())
    println("Done setup")
  }


}
