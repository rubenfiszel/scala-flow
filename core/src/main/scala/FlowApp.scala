package dawn.flow

trait FlowApp[M] extends App {

  implicit val modelHook = ModelHook[M]

  def drawExpandedGraph() =
    PrimaryNodeHook.drawGraph()

  def run(m: M) = {
    modelHook.setModel(m)
    PrimarySchedulerHook.run()
  }

}
