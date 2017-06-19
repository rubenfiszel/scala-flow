package dawn.flow

class InitHook[T] {

  var init: Option[T] = None
  def setInit(m: T) =
    init = Some(m)
}

object RequireInit {
  def isRequiring(x: Any) = x match {
    case x: RequireInit[_] => true
    case _ => false
  }
}

trait RequireInit[M] {

  def initHook: InitHook[M]
  def init = initHook.init.get

}
