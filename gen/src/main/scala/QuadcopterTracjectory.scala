package spatial.fusion.gen

import spire.math._
import spire.implicits._
import breeze.linalg.{max => _, min => _, _ => _}
//import breeze.math._
//Author: Ruben Fiszel <ruben.fiszel@epfl.ch>
//Inspired from RapidTrajectory from Mark W. Mueller <mwm@mwm.im>
case class SingleAxisInit(p: Real, v: Real, a: Real)
case class SingleAxisGoal(p: Option[Real], v: Option[Real], a: Option[Real])

case class SingleAxisTrajectory(init: SingleAxisInit,
                                goal: SingleAxisGoal,
                                tf: Timeframe) {

  val pf = goal.p.getOrElse(0.0)
  val vf = goal.v.getOrElse(0.0)
  val af = goal.a.getOrElse(0.0)

  val p0 = init.p
  val v0 = init.v
  val a0 = init.a

  val T1 = tf
  val T2 = T1 * T1
  val T3 = T2 * T1
  val T4 = T3 * T1
  val T5 = T4 * T1

  val da = af - a0
  val dv = vf - v0 - a0 * T1
  val dp = pf - p0 - v0 * T1 - 0.5 * a0 * T1 * T1

  //solve the trajectories, depending on what's constrained:
  lazy val (a: Real, b: Real, g: Real) =
    goal match {
      case SingleAxisGoal(Some(_), Some(_), Some(_)) =>
        ((60 * T2 * da - 360 * T1 * dv + 720 * 1 * dp) / T5,
         (-24 * T3 * da + 168 * T2 * dv - 360 * T1 * dp) / T5,
         (3 * T4 * da - 24 * T3 * dv + 60 * T2 * dp) / T5)
      case SingleAxisGoal(Some(_), Some(_), None) =>
        ((-120 * T1 * dv + 320 * dp) / T5,
         (72 * T2 * dv - 200 * T1 * dp) / T5,
         (-12 * T3 * dv + 40 * T2 * dp) / T5)
      case SingleAxisGoal(Some(_), None, Some(_)) =>
        ((-15 * T2 * da + 90 * dp) / (2 * T5),
         (15 * T3 * da - 90 * T1 * dp) / (2 * T5),
         (-3 * T4 * da + 30 * T2 * dp) / (2 * T5))
      case SingleAxisGoal(None, Some(_), Some(_)) =>
        (0, (6 * T1 * da - 12 * dv) / T3, (-2 * T2 * da + 6 * T1 * dv) / T3)
      case SingleAxisGoal(Some(_), None, None) =>
        (20 * dp / T5, -20 * dp / T4, 10 * dp / T3)
      case SingleAxisGoal(None, Some(_), None) =>
        (0, -3 * dv / T3, 3 * dv / T2)
      case SingleAxisGoal(None, None, Some(_)) =>
        (0, 0, da / T1)
      case _ =>
        (0, 0, 0)
    }

  lazy val cost = (g ** 2) + b * g * T1 + (b ** 2) * T2 / 3.0 + a * g * T2 / 3.0 + a * b * T3 / 4.0 + (a ** 2) * T4 / 20.0
  //Return the scalar jerk at time t.
  def getJerk(t: Time) =
    g + b * t + (1.0 / 2.0) * a * t * t

  //Return the scalar acceleration at time t.
  def getAcceleration(t: Time) =
    a0 + g * t + (1.0 / 2.0) * b * t * t + (1.0 / 6.0) * a * t * t * t

  //Return the scalar velocity at time t.
  def getVelocity(t: Time) =
    v0 + a0 * t + (1.0 / 2.0) * g * t * t + (1.0 / 6.0) * b * t * t * t + (1.0 / 24.0) * a * t * t * t * t

  //Return the scalar position at time t.
  def getPosition(t: Time) =
    p0 + v0 * t + (1.0 / 2.0) * a0 * t * t + (1.0 / 6.0) * g * t * t * t + (1.0 / 24.0) * b * t * t * t * t + (1.0 / 120.0) * a * t * t * t * t * t

  lazy val accPeakTimes: List[Real] = {
    if (a != 0) {
      val det = b * b - 2 * g * a
      if (det < 0)
        List(0, 0)
      else
        List((-b + sqrt(det)) / a, (-b - sqrt(det)) / a)
    } else {
      if (b != 0)
        List(-g / b, 0)
      else
        List(0, 0)
    }
  }

  //Return the extrema of the acceleration trajectory between t1 and t2.
  def getMinMaxAcc(t1: Time, t2: Time) = {

    //Evaluate the acceleration at the boundaries of the period:
    var aMinOut = Math.min(getAcceleration(t1), getAcceleration(t2))
    var aMaxOut = max(getAcceleration(t1), getAcceleration(t2))

    //#Evaluate at the maximum/minimum times:
    for (apt <- accPeakTimes) {
      if (apt > t1 && apt < t2) {
        aMinOut = Math.min(aMinOut, getAcceleration(apt))
        aMaxOut = max(aMaxOut, getAcceleration(apt))
      }
    }

    (aMinOut, aMaxOut)
  }

  //Return the extrema of the jerk squared trajectory between t1 and t2.
  def getMaxJerkSquared(t1: Real, t2: Real) = {
    var r = max(getJerk(t1) ** 2, getJerk(t2) ** 2)
    if (a != 0) {
      val tMax = -b / a
      if (tMax > t1 && tMax < t2)
        max(getJerk(tMax) ** 2, r)
      else
        r
    } else
      r
  }

}

case class Vec3(x: Real, y: Real, z: Real)
    extends DenseVector[Real](Array(x, y, z))

object Vec3 {
  def zero = Vec3(0, 0, 0)
  def one = Vec3(1, 1, 1)
}

case class Init(p: Vec3, v: Vec3, a: Vec3) {
  def apply(i: Int) = SingleAxisInit(p(i), v(i), a(i))
}

object Init {
  def zero = Init(Vec3.zero, Vec3.zero, Vec3.zero)
}

case class Goal(p: Option[Vec3], v: Option[Vec3], a: Option[Vec3]) {
  def apply(i: Int) = SingleAxisGoal(p.map(_(i)), v.map(_(i)), a.map(_(i)))
}

object Goal {
  def one = Goal(Some(Vec3.one), Some(Vec3.one), Some(Vec3.one))
}

sealed trait Feasibility
case object ThrustHigh     extends Feasibility
case object ThrustLow      extends Feasibility
case object Indeterminable extends Feasibility
case object Feasible       extends Feasibility

case class RapidTrajectory(init: Init = Init.zero, goal: Goal = Goal.one, g: Vec3 = Vec3(0, 0, 9.81), tf: Timeframe = 1.0) {
  lazy val axis = (0 to 2).map(i => SingleAxisTrajectory(init(i), goal(i), tf))

  def toVec(is: IndexedSeq[Real]): Vec3 =
    Vec3(is(0), is(1), is(2))

  def toVec(is: DenseVector[Real]): Vec3 =
    Vec3(is(0), is(1), is(2))

  // Return the trajectory's 3D jerk value at time `t`.
  def getJerk(t: Time) =
    toVec(axis.map(_.getJerk(t)))

  def getAcceleration(t: Time) =
    toVec(axis.map(_.getAcceleration(t)))

  def getVelocity(t: Time) =
    toVec(axis.map(_.getVelocity(t)))

  def getPosition(t: Time) =
    toVec(axis.map(_.getPosition(t)))

  def getNormalVector(t: Time) =
    normalize(getAcceleration(t) - g)

  def getThrust(t: Time): Thrust =
    norm((getAcceleration(t) - g))

  def getBodyRates(t: Time, dt: Timeframe = 1e-3): Vec3 = {
    val n0 = DenseVector(getNormalVector(t).toArray)
    val n1 = DenseVector(getNormalVector(t + dt).toArray)

    //direction of omega, in inertial axes
    val crossProd = cross(n0, n1)
    if (norm(crossProd) > 1e-6) {
      val sc = n0.dot(n1) / dt
      toVec((normalize(crossProd) :* 2.0).map(acos(_)))
    } else
      Vec3(0, 0, 0)
  }

  lazy val cost =
    axis.map(_.cost).sum

  def isFeasible(fminAllowed: Thrust,
                 fmaxAllowed: Thrust,
                 wmaxAllowed: Omega,
                 minTimeSection: Timeframe): Feasibility = {

    def section(t1: Time, t2: Time): Feasibility = {
      if ((t2 - t1) < minTimeSection)
        Indeterminable
      else if (max(getThrust(t1), getThrust(t2)) > fmaxAllowed)
        ThrustHigh
      else if (min(getThrust(t1), getThrust(t2)) < fminAllowed)
        ThrustLow
      else {
        var fminSqr = 0.0
        var fmaxSqr = 0.0
        var jmaxSqr = 0.0

        axis.zip(g.toArray).foreach {
          case (ax, g) => {
            val (amin, amax) = ax.getMinMaxAcc(t1, t2)
            val v1           = amin - g
            val v2           = amax - g

            if (max(v1 ** 2, v2 ** 2) > fmaxAllowed ** 2)
              return ThrustHigh

            if (v1 * v2 < 0) {
              //sign of acceleration changes, so we've gone through zero
              fminSqr += 0
            } else
              fminSqr += min(abs(v1), abs(v2)) ** 2

            fmaxSqr += max(abs(v1), abs(v2)) ** 2

            jmaxSqr += ax.getMaxJerkSquared(t1, t2)

          }
        }
        val fmin = sqrt(fminSqr)
        val fmax = sqrt(fmaxSqr)

        if (fmax < fminAllowed)
          return ThrustLow
        if (fmin > fmaxAllowed)
          return ThrustHigh

        val wBound =
          if (fminSqr > 1e-6)
            sqrt(jmaxSqr / fminSqr)//#the 1e-6 is a divide-by-zero protection
          else
            Double.PositiveInfinity 

        if ((fmin < fminAllowed) || (fmax > fmaxAllowed) || (wBound > wmaxAllowed)) {
          val tHalf = (t1 + t2) / 2.0
          val r1    = section(t1, tHalf)

          if (r1 == Feasible)
            return section(tHalf, t2)
          else
            return r1
        }

        return Feasible
      }

    }

    section(0, tf)
  }

}
