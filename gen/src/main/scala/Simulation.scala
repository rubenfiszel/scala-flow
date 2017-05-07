package spatial.fusion.gen

trait Simulation {
  def universe: Universe
  def generators: Seq[Generator]

  def simulate(dt: Timestep, totalTime: Timestep, seed: Seed) = {

    var elapsed: Timestep = 0
    var retrieved         = List[Data]()
    var states            = List[State]()

    states ::= universe.reset(seed)

    def retrieveData() =
      retrieved :::= generators
        .flatMap(_.generate(universe, elapsed, dt))
        .toList

    retrieveData()
    while (elapsed < totalTime) {
      states ::= universe.update(dt)
      elapsed += dt
      retrieveData()
    }
    (states, retrieved)
  }

}
