package dawn.flow

class ModelCallBack[A] extends (A => Unit) {

  var callBack: A => Unit = (x: A) => ()

  def addCallback(f: A => Unit) = {
    val pCallBack = callBack
    callBack = (x: A) => {
      pCallBack(x)
      f(x)
    }
  }

  def apply(x: A) =
    callBack(x)

}

object RequireModel {
  def isRequiring(x: Any) = x match {
    case x: RequireModel[_] => true
    case _ => false
  }
}

trait RequireModel[M] {

  var model: Option[M] = None
  def mc: ModelCallBack[M]

  mc.addCallback((x: M) => {
    model = Some(x)
  })
}

object ModelCB {
  def apply[A](implicit ev: A <:< Model) = new ModelCallBack[A]()
}

