package dawn.flow

class ModelCallBack[A] extends (A => Unit) {

  var callBack: A => Unit = (x: A) => ()

  def addCallback(f: A => Unit) = {
    callBack = (x: A) => {
      callBack(x)
      f(x)
    }
  }

  def apply(x: A) =
    callBack(x)

}
