package dawn.flow

import com.github.mdr.ascii.graph._
import com.github.mdr.ascii.layout.GraphLayout
import com.github.mdr.ascii.layout.prefs._
trait NodeHook {

  var nodes = List[Node]()

  def addNode(s: Node) =
    nodes ::= s

  def getSources(n: Node) = n match {
    case x: Buffer[_] => List()
    case _ => n.sources
  }
  
  def drawGraph(blocks: Boolean = true, replays: Boolean = false) = {
    val blcks = nodes.filter(_.isInstanceOf[Block[_]]).map(_.asInstanceOf[Block[_]])
    def findInners(b: Block[_]) = {      
      def rec(sn: Node, end: List[Node]): List[Node] =
        sn :: getSources(sn).filterNot(end.contains(_)).flatMap(x => rec(x, end)).toList
      b.trans :: rec(b.out, b.sources.toList)
    }

    val inners = blcks.map(findInners(_))
    val innersF = inners.flatten

    def skipReplays(x: Node) = x match {
      case x: Replay[_] if !replays => x.source1
      case x: ReplayWithScheduler[_] if !replays => x.source1
      case _ => x
    }

    def filterReplays(x: Node) = x match {
      case x: Replay[_] if !replays => false
      case x: ReplayWithScheduler[_] if !replays => false
      case _ => true
    }
    val n = nodes.filterNot(innersF.contains(_)).filter(filterReplays)
    val graph  = Graph(n.toSet, n.flatMap(x => x.sources.map(skipReplays).filterNot(innersF.contains(_)).filter(filterReplays).map(y => (y, x))))
    val layout   = GraphLayout.renderGraph(graph)
    println(layout)

    if (blocks) {
      inners.zip(blcks).foreach { case (inn, blk) => {
        val innS = blk.sources.toList ::: inn
        val n = innS.filter(filterReplays)
        val graph  = Graph(n.toSet, n.flatMap(x => x.sources.map(skipReplays).filter(innS.contains(_)).filter(filterReplays).map(y => (y, x))))
        val layout   = GraphLayout.renderGraph(graph, layoutPrefs = LayoutPrefsImpl().copy(vertical = false))
        println(blk)
        println(layout)        
      }}
    }

  }

  //Expand replay blocks
  def expand() = {
    //Trick to access once sources and generate Replay op
    val length = nodes.flatMap(_.sources).length
    println("Done expansion " + length + " blocks")
  }

  def setup() = {
    expand()    
    val graph  = Graph(nodes.toSet, nodes.flatMap(x => getSources(x).map(y => (x, y))))
    val sorted = GraphUtils.topologicalSort(graph).get.reverse
    sorted.foreach(_.setup())
    println("Done setup")
  }

}
