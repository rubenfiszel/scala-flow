#!/usr/bin/env sage

from sage.all import *

Q.<i,j,k> = QuaternionAlgebra(SR, -1, -1)

var('q0, q1, q2, q3')
var('dt')
var('wx, wy, wz')

q = q0 + q1*i + q2*j + q3*k

w = vector([wx, wy, wz])*dt
w_norm = sqrt(w[0]^2 + w[1]^2 + w[2]^2)
ang = w_norm/2
w_normalized = w/w_norm
sin2 = sin(ang)
qd = cos(ang) + w_normalized[0]*sin2*i + w_normalized[1]*sin2*j + w_normalized[2]*sin2*k

nq = q*qd

#v = vector(nq.coefficient_tuple()).column()
v = vector(nq.coefficient_tuple())

for sym in [wx, wy, wz, q0, q1, q2, q3]:
    d = diff(v, sym)
    exps = map(lambda x: x.canonicalize_radical().full_simplify(), d)
    for i, e in enumerate(exps):
        print(sym, i, e) 
#ds = d.map(lambda x: x
