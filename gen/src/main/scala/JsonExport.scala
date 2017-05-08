package spatial.fusion.gen

import io.circe.syntax._

object JsonExport {
  def export(
    sim: Simulation,
    dt: Timestep = 0.01,
    seed: Seed = 12345) = {

    val datas = sim.simulate(dt, seed)
    datas.asJson
  }
}
