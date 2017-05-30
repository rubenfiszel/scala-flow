package dawn.flow

trait NodeHook {

  var l = List[Node]()

  def addNode(s: Node) =
    l ::= s

  def drawGraph() =
    Node.drawGraph(l.toSeq)
}
