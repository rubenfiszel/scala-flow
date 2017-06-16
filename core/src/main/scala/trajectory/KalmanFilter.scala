package dawn.flow

import breeze.linalg._

object KalmanFilter {

  def predict(xp: MatrixR, sp: MatrixR, f: MatrixR, u: MatrixR, q: MatrixR) = {
    val xm = f*xp + u
    val sigm = f*inv(sp)*f.t + q
    (xm, sigm)
  }

  def update(xm: MatrixR, sigm: MatrixR, z: MatrixR, h: MatrixR, r: MatrixR) = {
    val s = h*sigm*h.t + r
    val za = h*xm
    val k = sigm*h.t*inv(s)
    val sig = sigm + k*s*k.t
    val x = xm + k*(z - za)
    (x, sig)
  }
}
