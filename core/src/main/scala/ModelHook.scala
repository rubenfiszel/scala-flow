package dawn.flow

class ModelHook[A] extends (A => Unit) {

  var hook: A => Unit = (x: A) => ()

  def addHook(f: A => Unit) = {
    val pHook = hook
    hook = (x: A) => {
      pHook(x)
      f(x)
    }
  }

  def apply(x: A) =
    hook(x)

}

object ModelHook {
  def apply[A] = new ModelHook[A]()
}

object RequireModel {
  def isRequiring(x: Any) = x match {
    case x: RequireModel[_] => true
    case x: NamedFunction[_, _] => x.requireModel
    case _ => false
  }
}

trait RequireModel[M] {

  var model: Option[M] = None
  def modelHook: ModelHook[M]

  modelHook.addHook((x: M) => {
    model = Some(x)
  })
}

