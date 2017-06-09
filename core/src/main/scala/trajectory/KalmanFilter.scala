package dawn.flow

import breeze.linalg._

object KalmanFilter {

  def predict(xp: VectorR, sp: MatrixR, u: VectorR, f: MatrixR, b: MatrixR, q: MatrixR) = {
    val xm = f*xp + b*u
    val sigm = f*inv(sp)*f.t + q
    (xm, sigm)
  }

  def update(xm: VectorR, z: VectorR, sigm: MatrixR, h: MatrixR, r: MatrixR) = {
    val s = h*sigm*h.t + r
    val za = h*xm
    val k = sigm*h*inv(s)
    val sig = sigm + k*s*k.t
    val x = xm + k*(z - za)
    (x, sig)
  }
}
