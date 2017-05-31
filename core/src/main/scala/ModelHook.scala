package dawn.flow

class ModelHook[A] {

  var model: Option[A] = None
  def setModel(m: A) =
    model = Some(m)
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

  def modelHook: ModelHook[M]
  def model = modelHook.model

}
