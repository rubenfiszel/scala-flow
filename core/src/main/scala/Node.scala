package dawn.flow


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

  def addParams(s: Node): Set[Any] = s match {
    case x: Product => x.productIterator.toSet
    case _ => Set()
  }
}
