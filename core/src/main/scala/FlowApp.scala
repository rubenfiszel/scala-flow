package dawn.flow

trait FlowApp[M, I] extends App {

  implicit val modelHook = new ModelHook[M] {}
  implicit val initHook = new InitHook[I]

  def drawExpandedGraph() =
    PrimaryNodeHook.drawGraph()

  def run(m: M, i: I) = {
    modelHook.setModel(m)
    initHook.setInit(i)
    PrimarySchedulerHook.run()
  }

}
