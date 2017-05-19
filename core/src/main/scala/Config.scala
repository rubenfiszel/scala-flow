package dawn.flow

object Config {

  // If you desactivate it, make sure you have cache at every duplication of stream
  // In particular for buffer, else it will stack overflow or at least
  // calculate a new stream for EACH point
  val CACHE_BY_DEFAULT = true

}
