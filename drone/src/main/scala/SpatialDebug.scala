package dawn.flow.spatial

import breeze.linalg.{DenseMatrix, inv, det}
import spatial.dsl._
import dawn.flow.spatialf._
import dawn.flow._
import org.virtualized._

object SpatialDebug extends FlowApp[Any, Any] {

  val clock = new Clock(1).stop(1)

  val spatial = new SpatialBatch1[Time, Time, Double, Double](clock) {

    val N: scala.Int = 20

    val initV: (scala.Double, scala.Double, scala.Double)               = (0.0, 0.0, 0.0)
    val initP: (scala.Double, scala.Double, scala.Double)               = (0.0, 0.0, 0.0)
    val initQ: (scala.Double, scala.Double, scala.Double, scala.Double) = (1.0, 0.0, 0.0, 0.0)
    val initCov = 0.00001

    val initTime: scala.Double  = 0.0
    val covGyro: scala.Double   = 0.01
    val covAcc: scala.Double    = 0.01
    val covViconP: scala.Double = 0.01
    val covViconQ: scala.Double = 0.01
    

    type Real         = Float
    type Time         = Real
    type Position     = Vec3
    type Velocity     = Vec3
    type Acceleration = Vec3
    type Omega        = Vec3
    type Attitude     = Quat

    @struct case class Quat(r: Real, i: Real, j: Real, k: Real)

    //For matrices, the vecs are vector columns e.g Mat33(Vec3, Vec3, Vec3)
    //where Vec3 is 3x1
    @struct case class Vec3(x: Real, y: Real, z: Real)
    @struct case class Vec6(a: Real, b: Real, c: Real, d: Real, e: Real, f: Real)

    case class Mat33(reg: RegFile2[Real])
    case class Mat63(reg: RegFile2[Real])
    case class Mat36(reg: RegFile2[Real])
    case class Mat66(reg: RegFile2[Real])

    @struct case class IMU(t: Time, a: Acceleration, g: Omega)
    @struct case class POSE(p: Vec3, q: Attitude)
    @struct case class Vicon(t: Time, pose: POSE)
    @struct case class Sigma(a: Vec6, b: Vec6, c: Vec6, d: Vec6, e: Vec6, f: Vec6)
    @struct case class State(x: Vec6, sig: Sigma)
    //x(v1, v2, v3, p1, p2, p3)
    @struct case class Particle(w: Real, q: Quat, st: State, lastA: Acceleration, lastQ: Quat)

    /* Mat33(
     0, 1, 2,
     3, 4, 5
     6, 7, 8
     )
     */
    def createReg(a: scala.Int, b: scala.Int, elems: Seq[Real]): RegFile2[Real] = {
      val r = RegFile[Real](a, b)
      Pipe {
        List
          .tabulate(a*b)(i => i)
          .foreach(i => Pipe { r(i/b, i%b) = elems(i) })
      }
      r
    }
    def createMat33(elems: Real*): Mat33 =
      Mat33(createReg(3, 3, elems))

    def createMat36(elems: Real*): Mat36 =
      Mat36(createReg(3, 6, elems))

    def createMat63(elems: Real*): Mat63 =
      Mat63(createReg(6, 3, elems))

    def createMat66(elems: Real*): Mat66 =
      Mat66(createReg(6, 6, elems))

    def add(h: scala.Int, w: scala.Int, from: RegFile2[Real], to: RegFile2[Real]) = {
      Foreach(h by 1, w by 1) { (i, j) =>
        to(i, j) = to(i, j) + from(i, j)
      }
    }

    def add(h: scala.Int, from: RegFile1[Real], to: RegFile1[Real]) = {
      Foreach(h by 1) { i =>
        to(i) = to(i) + from(i)
      }
    }
    
    def zero(h: scala.Int, w: scala.Int, out: RegFile2[Real]) = {
      Foreach(h by 1, w by 1) { (i, j) =>
        out(i, j) = 0
      }
    }

    def zero(h: scala.Int, out: RegFile1[Real]) = {
      Foreach(h by 1) { i =>
        out(i) = 0
      }
    }
    
    def mult3(b: scala.Int, cols: RegFile2[Real], rows: RegFile1[Real]): Vec3 = {
      val a: scala.Int = 3
      val sum = RegFile[Real](a)
      zero(a, sum)
      Foreach(b by 1) { i => {
        val col = Vec3(cols(0, i), cols(1, i), cols(2, i))
        val row = rows(i)
        val out = (col*row).reg
        add(a, out, sum)
      }}
      Vec3(sum(0), sum(1), sum(2))
    }

    def mult6(b: scala.Int, cols: RegFile2[Real], rows: RegFile1[Real]): Vec6 = {
      val a: scala.Int = 6
      val sum = RegFile[Real](a)
      zero(a, sum)
      Foreach(b by 1) { i => {
        val col = Vec6(cols(0, i), cols(1, i), cols(2, i), cols(3, i), cols(4, i), cols(5, i))
        val row = rows(i)
        val out = (col*row).reg
        add(a, out, sum)
      }}
      Vec6(sum(0), sum(1), sum(2), sum(3), sum(4), sum(5))
    }
    
    def mult33(b: scala.Int, cols: RegFile2[Real], rows: RegFile2[Real]): Mat33 = {
      val a: scala.Int = 3
      val c: scala.Int = 3
      val sum = RegFile[Real](a, c)
      zero(a, c, sum)
      Foreach(b by 1) { i => {
        val out = RegFile[Real](a, c)
        val col = Vec3(cols(0, i), cols(1, i), cols(2, i))
        val row = Vec3(rows(i, 0), rows(i, 1), rows(i, 2))
        col.outerProd(row, out)
        add(a, c, out, sum)
      }}
      Mat33(sum)
    }

    def mult36(b: scala.Int, cols: RegFile2[Real], rows: RegFile2[Real]): Mat36 = {
      val a: scala.Int = 3
      val c: scala.Int = 6
      val sum = RegFile[Real](a, c)
      zero(a, c, sum)
      Foreach(b by 1) { i => {
        val out = RegFile[Real](a, c)
        val col = Vec3(cols(0, i), cols(1, i), cols(2, i))
        val row = Vec6(rows(i, 0), rows(i, 1), rows(i, 2), rows(i, 3), rows(i, 4), rows(i, 5))
        col.outerProd(row, out)
        add(a, c, out, sum)
      }}
      Mat36(sum)
    }

    def mult63(b: scala.Int, cols: RegFile2[Real], rows: RegFile2[Real]): Mat63 = {
      val a: scala.Int = 6
      val c: scala.Int = 3
      val sum = RegFile[Real](a, c)
      zero(a, c, sum)
      Foreach(b by 1) { i => {
        val out = RegFile[Real](a, c)
        val col = Vec6(cols(0, i), cols(1, i), cols(2, i), cols(3, i), cols(4, i), cols(5, i))
        val row = Vec3(rows(i, 0), rows(i, 1), rows(i, 2))
        col.outerProd(row, out)
        add(a, c, out, sum)
      }}
      Mat63(sum)
    }
    
    def mult66(b: scala.Int, cols: RegFile2[Real], rows: RegFile2[Real]): Mat66 = {
      val a: scala.Int = 6
      val c: scala.Int = 6
      val sum = RegFile[Real](a, c)
      zero(a, c, sum)
      Foreach(b by 1) { i => {
        val out = RegFile[Real](a, c)
        val col = Vec6(cols(0, i), cols(1, i), cols(2, i), cols(3, i), cols(4, i), cols(5, i))
        val row = Vec6(rows(i, 0), rows(i, 1), rows(i, 2), rows(i, 3), rows(i, 4), rows(i, 5))
        col.outerProd(row, out)
        add(a, c, out, sum)
      }}
      Mat66(sum)
    }

    def transposeReg(a:scala.Int, b:scala.Int, r: RegFile2[Real]): RegFile2[Real] = {
      val out = RegFile[Real](b, a)
      Foreach(a by 1, b by 1){ (i, j) =>
        out(j, i) = r(i, j)
      }
      out
    }

    def addRegs(a:scala.Int, b:scala.Int, r1: RegFile2[Real], r2: RegFile2[Real]): RegFile2[Real] = {
      val out = RegFile[Real](a, b)
      Foreach(a by 1, b by 1){ (i, j) =>
        out(i, j) = r1(i, j) + r2(i, j)
      }
      out
    }

    def subRegs(a:scala.Int, b:scala.Int, r1: RegFile2[Real], r2: RegFile2[Real]): RegFile2[Real] = {
      val out = RegFile[Real](a, b)
      Foreach(a by 1, b by 1){ (i, j) =>
        out(i, j) = r1(i, j) - r2(i, j)
      }
      out
    }

    def multReg(a:scala.Int, b:scala.Int, r: RegFile2[Real], factor: Real): RegFile2[Real] = {
      Foreach(a by 1, b by 1){ (i, j) =>
        r(i, j) = r(i, j) * factor
      }
      r
    }
    
    implicit class Mat33Ops(m: Mat33) {

      def apply(y: scala.Int, x: scala.Int) =
        m.reg(y, x)

      def t: Mat33 =
        Mat33(transposeReg(3, 3, m.reg))

      def +(y: Mat33): Mat33 =
        Mat33(addRegs(3, 3, m.reg, y.reg))

      def *(y: Real): Mat33 =
        Mat33(multReg(3, 3, m.reg, y))
      def *(y: Vec3): Vec3 =
        mult3(3, m.reg, y.reg)
      def *(y: Mat33): Mat33 =
        mult33(3, m.reg, y.reg)
      def *(y: Mat36): Mat36 =
        mult36(3, m.reg, y.reg)
    }

    implicit class Mat36Ops(m: Mat36) {
      def apply(y: scala.Int, x: scala.Int) =
        m.reg(y, x)

      def t: Mat63 =
        Mat63(transposeReg(3, 6, m.reg))
      def *(y: Mat66): Mat36 =
        mult36(6, m.reg, y.reg)
      def *(y: Mat63): Mat33 =
        mult33(3, m.reg, y.reg)
      def *(y: Vec6): Vec3 =
        mult3(6, m.reg, y.reg)
    }

    implicit class Mat63Ops(m: Mat63) {

      def apply(y: scala.Int, x: scala.Int) =
        m.reg(y, x)

      def t: Mat36 =
        Mat36(transposeReg(6, 3, m.reg))
      def *(y: Mat33): Mat63 =
        mult63(3, m.reg, y.reg)
      def *(y: Mat36): Mat66 =
        mult66(3, m.reg, y.reg)
      def *(y: Vec3): Vec6   =
        mult6(3, m.reg, y.reg)
      
    }
    
    implicit class Mat66Ops(m: Mat66) {

      def apply(y: scala.Int, x: scala.Int) =
        m.reg(y, x)
      
      def t: Mat66 =
        Mat66(transposeReg(6, 6, m.reg))

      def *(y: Vec6): Vec6   =
        mult6(6, m.reg, y.reg)
      def *(y: Mat66): Mat66 =
        mult66(6, m.reg, y.reg)
      def *(y: Mat63): Mat63 =
        mult63(6, m.reg, y.reg)
      def +(y: Mat66): Mat66 =
        Mat66(addRegs(6, 6, m.reg, y.reg))
      def -(y: Mat66): Mat66 =
        Mat66(subRegs(6, 6, m.reg, y.reg))
    }


    def createVec3(elems: Real*) = Vec3(elems(0), elems(1), elems(2))
    implicit class Vec3Ops(x: Vec3) {
      def *(y: Real) = Vec3(x.x * y, x.y * y, x.z * y)
      def dot(y: Vec3)      = x.x * y.x + x.y * y.y + x.z * y.z
      

      def outerProd(y: Vec3, out: RegFile2[Real]) = {
        val xr = x.reg
        val yr = y.reg
        Foreach(9 by 1) { i =>
          out((i / 3), (i % 3)) = xr(i / 3) * yr(i % 3)
        }
      }
      
      def outerProd(y: Vec6, out: RegFile2[Real]) = {
        val xr = x.reg
        val yr = y.reg
        Foreach(18 by 1) { i =>
          out((i / 6), (i % 6)) = xr(i / 6) * yr(i % 6)
        }
      }
      
      @virtualize def reg = {
        val rg = RegFile[Real](3)
        Pipe {
          List
            .tabulate(3)(i => i)
            .foreach(i => Pipe { rg(i) = x(i) })
        }
        rg
      }
      def apply(i: scala.Int) = i match {
        case 0 => x.x
        case 1 => x.y
        case 2 => x.z
      }
      def +(y: Real) = Vec3(x.x + y, x.y + y, x.z + y)
      def +(y: Vec3) = Vec3(x.x + y.x, x.y + y.y, x.z + y.z)
      def -(y: Vec3) = Vec3(x.x - y.x, x.y - y.y, x.z - y.z)
    }

    def createVec6(elems: Real*) = Vec6(elems(0), elems(1), elems(2), elems(3), elems(4), elems(5))

    implicit class Vec6Ops(x: Vec6) {
      def vec3a = Vec3(x.a, x.b, x.c)
      def *(y: Real): Vec6 =
        createVec6(List.tabulate(6)(i => x(i) * y): _*)
      def +(y: Vec6): Vec6 =
        createVec6(List.tabulate(6)(i => x(i) + y(i)): _*)
      def -(y: Vec6): Vec6 =
        createVec6(List.tabulate(6)(i => x(i) - y(i)): _*)

      def outerProd(y: Vec6, out: RegFile2[Real]) = {
        val xr = x.reg
        val yr = y.reg
        Foreach(36 by 1) { i =>
          out((i / 6), (i % 6)) = xr(i / 6) * yr(i % 6)
        }
      }

      def outerProd(y: Vec3, out: RegFile2[Real]) = {
        val xr = x.reg
        val yr = y.reg
        Foreach(18 by 1) { i =>
          out((i / 3), (i % 3)) = xr(i / 3) * yr(i % 3)
        }
      }

      @virtualize def reg = {
        val rg = RegFile[Real](6)
        Pipe {
          List
            .tabulate(6)(i => i)
            .foreach(i => Pipe{rg(i) = x(i)})
        }
        rg
      }
      def apply(i: scala.Int) = i match {
        case 0 => x.a
        case 1 => x.b
        case 2 => x.c
        case 3 => x.c
        case 4 => x.c
        case 5 => x.c
      }

    }

    def fromMat66(x: Mat66): Sigma = {
      def row(i: scala.Int): Vec6 = Vec6(x.reg(i, 0), x.reg(i, 1), x.reg(i, 2), x.reg(i, 3), x.reg(i, 4), x.reg(i, 5))
      Sigma(row(0), row(1), row(2), row(3), row(4), row(5))
    }

    implicit class SigmaOps(sig: Sigma) {
      def toMat66: Mat66 =
        createMat66(List.tabulate(36)(i => sig(i/6, i%6)):_*)
      def apply(y: scala.Int, x: scala.Int) = (x match {
        case 0 => sig.a
        case 1 => sig.b
        case 2 => sig.c
        case 3 => sig.d
        case 4 => sig.e
        case 5 => sig.f
      })(y)
    }

    implicit class QuatOps(x: Quat) {
      def *(y: Real)        = Quat(x.r * y, x.i * y, x.j * y, x.k * y)
      def *(y: Quat)        = QuatMult(x, y)
      def dot(y: Quat)      = x.r * y.r + x.i * y.i + x.j * y.j + x.k * y.k
      def rotateBy(q: Quat) = q * x
      def rotate(v: Vec3): Vec3 = {
        val inv = x.inverse
        val nq  = (x * Quat(0.0, v.x, v.y, v.z)) * inv
        Vec3(nq.i, nq.j, nq.k)
      }
      def inverse = QuatInverse(x)
    }

  def QuatMult(q1: Quat, q2: Quat) = {
    Quat(
      q1.r * q2.r - q1.i * q2.i - q1.j * q2.j - q1.k * q2.k,
      q1.r * q2.i + q1.i * q2.r + q1.j * q2.k - q1.k * q2.j,
      q1.r * q2.j - q1.i * q2.k + q1.j * q2.r + q1.k * q2.i,
      q1.r * q2.k + q1.i * q2.j - q1.j * q2.i + q1.k * q2.r
    )
  }

    def QuatInverse(q: Quat) = {
      val n = q.r * q.r + q.i * q.i + q.j * q.j + q.k * q.k
      Quat(q.r, -q.i, -q.j, q.j) * (1 / n)
    }

  def det(a: Mat33): Real = {
    val (a11, a12, a13) = (a(0, 0), a(0, 1), a(0, 2))
    val (a21, a22, a23) = (a(1, 0), a(1, 1), a(1, 2))
    val (a31, a32, a33) = (a(2, 0), a(2, 1), a(2, 2))
    a11 * (a33 * a22 - a32 * a23) - a21 * (a33 * a12 - a32 * a13) + a31 * (a23 * a12 - a22 * a13)
  }

    def inv(a: Mat33): Mat33 = {
      val (a11, a12, a13) = (a(0, 0), a(0, 1), a(0, 2))
      val (a21, a22, a23) = (a(1, 0), a(1, 1), a(1, 2))
      val (a31, a32, a33) = (a(2, 0), a(2, 1), a(2, 2))

      val A = createMat33(
        a33*a22-a32*a23, -(a33*a12-a32*a13), a23*a12-a22*a13,
        -(a33*a21-a31*a23), a33*a11-a31*a13, -(a23*a11-a21*a13),
        a32*a21-a31*a22, -(a32*a11-a31*a12), a22*a11-a21*a12
      )
      
      A * (1 / det(a))
    }
    
    def normalLogPdf(measurement: Vec3, state: Vec3, cov: Mat33): Real = {
      val e = (measurement-state)
      -1/2.0*e.dot(inv(cov)*e)
    }

    @virtualize def initParticles(particles: SRAM1[Particle], parFactor: scala.Int) = {
      Foreach(N by 1 par parFactor)(x => {
        val initQuat = Quat(initQ._1, initQ._2, initQ._3, initQ._4)
        Parallel {
          particles(x) = Particle(
            math.log(1.0 / N),
            initQuat,
            State(
              Vec6(initV._1, initV._2, initV._3, initP._1, initP._2, initP._3),
              Sigma(
                Vec6(initCov, 0, 0, 0, 0, 0),
                Vec6(0, initCov, 0, 0, 0, 0),
                Vec6(0, 0, initCov, 0, 0, 0),
                Vec6(0, 0, 0, initCov, 0, 0),
                Vec6(0, 0, 0, 0, initCov, 0),
                Vec6(0, 0, 0, 0, 0, initCov)
              )
            ),
            Vec3(0.0, 0.0, 0.0),
            initQuat
          )

        }
      })
    }
    

    @virtualize def resample(particles: SRAM1[Particle], parFactor: Int) = {

      val weights = SRAM[Real](N)
      val out = SRAM[Particle](N)

      val u = random[Real](1.0)

      Foreach(N by 1)(i => {
        if (i == 0)
          weights(i) = exp(particles(i).w)
        else
          weights(i) = weights(i-1) + exp(particles(i).w)
      })

      val k = Reg[Int](0)
      Foreach(N by 1)(i => {
        val b = weights(k)*N < i.to[Real] + u
        FSM[Boolean, Boolean](b)(x => x)(x => k := k + 1)(x => weights(k)*N < i.to[Real]+u)
        out(i) = particles(k)
      })

      Foreach(N by 1 par parFactor)(i => {
        val p = out(i)
        particles(i) = Particle(log(1.0/N), p.q, p.st, p.lastA, p.lastQ)
      })

    }
    
    @virtualize def normWeights(particles: SRAM1[Particle], parFactor: Int) = {
      val totalWeight = Reg[Real](0)
      val maxR = Reg[Real]
      maxR := particles(0).w
      Reduce(maxR)(N by 1 par parFactor)(i => particles(i).w)((x,y) => max(x,y))
      println(maxR)
      Reduce(totalWeight)(N by 1 par parFactor)(i => exp(particles(i).w - maxR))(_+_)
      totalWeight := maxR + log(totalWeight)
      println(totalWeight.value)
      Foreach(N by 1 par parFactor)(i => {
        val p = particles(i)
        particles(i) = Particle(p.w - totalWeight, p.q, p.st, p.lastA, p.lastQ)
      })
    }


    @virtualize def printMat(mat33: Mat33) = {
      val s = List.tabulate(3)(j =>
        List.tabulate(3)( i => mat33.reg(j, i).toString).foldRight((" ": String))(_ + " " + _)
      ).foldRight((" ": String))(_ + "\n" + _)
      println(s)
    }
    @virtualize def printVec(v:Vec3) = {
      println(v.x + " " + v.y + " " + v.z)
    }
    def spatial(x: TSA) = {
      val particles = SRAM[Particle](N)

      val cov = Mat33(createReg(3, 3, Seq[Float](
        0.3, 0.1, 0.1,
        0.1, 0.5, 0.1,
        0.1, 0.1, 0.9)))

      initParticles(particles, 10)
      resample(particles, 10)
      normWeights(particles, 10)
      1.0
    }
  }

  //  spatial.debug


//  Plot(spatial)

//  drawExpandedGraph()

  run(null, null)

  val cov = DenseMatrix(
    (0.3, 0.1, 0.1),
    (0.1, 0.5, 0.1),
    (0.1, 0.1, 0.9))

  System.exit(0)

}


