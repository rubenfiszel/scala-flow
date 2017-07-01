#!/usr/bin/env sage

from sage.all import *

Q.<i,j,k> = QuaternionAlgebra(SR, -1, -1)

var('q0, q1, q2, q3')
var('dt')
var('wx, wy, wz, w_norm_sq')

q = q0 + q1*i + q2*j + q3*k

w = vector([wx, wy, wz])
w_norm = sqrt(w.dot_product(w))
ang = w_norm/2
w_normalized = w/w_norm
sin2 = sin(ang)
qd = cos(ang) + w_normalized[0]*sin2*i + w_normalized[1]*sin2*j + w_normalized[2]*sin2*k

nq = q*qd

#v = vector(nq.coefficient_tuple()).column()
v = vector(nq.coefficient_tuple())

for j, sym in enumerate([q0, q1, q2, q3]):
    d = diff(v, sym)
    for i, e in enumerate(d):
        e = e.canonicalize_radical()
        e = e.full_simplify()
        print('t({}, {}) = {}'.format(i, j, e) )

