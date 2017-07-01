
# This file was *autogenerated* from the file ./jacob2.sage
from sage.all_cmdline import *   # import sage library

_sage_const_2 = Integer(2); _sage_const_1 = Integer(1); _sage_const_0 = Integer(0)#!/usr/bin/env sage

from sage.all import *

Q = QuaternionAlgebra(SR, -_sage_const_1 , -_sage_const_1 , names=('i', 'j', 'k',)); (i, j, k,) = Q._first_ngens(3)

var('q0, q1, q2, q3')
var('dt')
var('wx, wy, wz, w_norm_sq')

q = q0 + q1*i + q2*j + q3*k

w = vector([wx, wy, wz])
w_norm = sqrt(w.dot_product(w))
ang = w_norm/_sage_const_2 
w_normalized = w/w_norm
sin2 = sin(ang)
qd = cos(ang) + w_normalized[_sage_const_0 ]*sin2*i + w_normalized[_sage_const_1 ]*sin2*j + w_normalized[_sage_const_2 ]*sin2*k

nq = q*qd

#v = vector(nq.coefficient_tuple()).column()
v = vector(nq.coefficient_tuple())

for j, sym in enumerate([q0, q1, q2, q3]):
    d = diff(v, sym)
    for i, e in enumerate(d):
        e = e.canonicalize_radical()
        e = e.full_simplify()
        print('t({}, {}) = {}'.format(i, j, e) )


