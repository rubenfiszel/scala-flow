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

trait RequireModel[M] {

  var model: Option[M] = None
  def mc: ModelCallBack[M]

  mc.addCallback((x: M) => {
    model = Some(x)
  })
}
