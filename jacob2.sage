#!/usr/bin/env sage

from sage.all import *

Q.<i,j,k> = QuaternionAlgebra(SR, -1, -1)

var('q0, q1, q2, q3')
var('ax, ay, az')

q = q0 + q1*i + q2*j + q3*k
v = ax*i + ay*j + az*k

nq = q*v*q.conjugate()

v = vector(nq.coefficient_tuple())
vs = map(lambda x: x.canonicalize_radical().full_simplify(), v)

for sym in [ax, ay, az, q0, q1, q2, q3]:
    d = diff(v, sym)
    exps = map(lambda x: x.canonicalize_radical().full_simplify(), d)[1:]
    for i, e in enumerate(exps):
        print(sym, i, e) 
