---
title: "Sensor fusion for drones: Rao-Blackwellized Particle filter for POSE and wind estimation"
author: Ruben Fiszel
affiliation: Stanford University
email: ruben.fiszel@epfl.ch
date: June 2017
link-citations: true
---

# Introduction

We will introduce here several filter for POSE estimation for highly dynamic objects and in particular drones. The order is from the most conceptually simple, to the most complex. The Rao-Blackwellized Particle filter can be viewed as a superset of many other filters, where those filters are just instances of a particle filter with only one particle.

## Drone

**TODO**: Talk about drone, high-dynamic objects

## Notes on notation and conventions

The referential by default is the fixed world frame. 

- $\mathbf{x}$ designates a vector
- $x_t$ is the random variable of x at time t
- $x_{t1:t2}$ is the product of the random variable of x between t1 included and t2 included
- $x^{(i)}$ designates the random variable x of the arbitrary particle i
- $\hat{x}$ designates an estimated variable

## POSE 

POSE is the task of determining the position and orientation of an object through time. It is a subroutine of SLAM (Software Localization And Mapping). We can formelize the problem as:

At each timestep, find the best expectation of a function of the hidden variable state (position and orientation), from their initial distribution and the history of observable random variables (such as sensor measurements).

- The state $\mathbf{x}$
- The function $g(\mathbf{x})$ such that $g(\mathbf{x}_t) = (\mathbf{p}_t, \mathbf{q}_t)$ where $\mathbf{p}$ is the position and $\mathbf{q}$ is the attitude as a quaternion.
- The observable variable $\mathbf{y}$ composed of the sensor measurements $\mathbf{z}$ and the control input $\mathbf{u}$

The algorithm inputs are:

- control inputs $\mathbf{u}_t$ (the commands sent to the flight controller)
- sensor measurements $\mathbf{z}_t$ coming from different sensors with different sampling rate
- information about the sensors (sensor measurements biases and matrix of covariance) 

### Wind

We will add one novel subtlety over vanilla POSE is that we would like to also keep track of the wind force. This would enable us to do smooth motion planning in windy environments instead of limiting ourselves to indoors.

## Quaternion

Quaternions are extensions of complex numbers but with 3 imaginary parts. Unit quaternions can be used to represent orientation, also referred to as attitude. Quaternions algebra make rotation composition simple and quaternions avoid the issue of gimbal lock. In all filters presented, they will be used to represent the attitude.

Quaternion rotations composition is: $q_2 q_1$ which results in $q_1$ being rotated by the rotation represented by $q_2$. From this, we can deduce that angular velocity integrated over time is simply $q^t$ if $q$ is the local quaternion rotation by unit of time.

Rotation of a vector by a quaternion is done by: $q v q^*$ where $q$ is the quaternion representing the rotation, $q^*$ its conjugate and $v$ the vector to be rotated.

### Average quaternion

It will be useful later to calculate the average quaternion of a set of quaternions. Unfortunately, the average of its components $\frac{1}{N} \sum q_i$ or *barycentric* mean is unsound: Indeed, attitude do not belong to a vector space but a homogenous Riemannian manifold (the four dimensional unit sphere). To convince yourself of the unsoundness of the *barycentric* mean, see that the addition and barycentric mean of two unit quaternion is not necessarily an unit quaternion ($(1, 0, 0, 0)$ and $(-1, 0, 0, 0)$ for instance. Furthermore, angle being periodic, the *barycentric* mean of a quaternion with angle $-178^\circ$ and another with same body-axis and angle $180^\circ$ gives $1^\circ$ instead of the expected $-179^\circ$. 

We present here two solutions to calculate the average quaternion: 

- Use the barycentric mean but negate the components of the quaternion that do not belong to the same demi-sphere as an arbitrary quaternion in the set. This solves the aberrant case above and is practical. It is a simplified form of the algorithm presented in [@markley_averaging_2007].
- Use the the intrinsic gradient descent algorithm [@xavier_computing_1998] . Starting with an abitrary mean $\bar{\mathbf{q}}$ (we use the one from the previously described method), we can iteratively improve the convergence between the quaternions. 
$$\mathbf{e_i} = \mathbf{q_i}\bar{\mathbf{q}}_t^{-1}$$ such that
$$\mathbf{q_i} = \mathbf{e_i}\bar{\mathbf{q}}$$
$$\mathbf{e} = \frac{1}{2n}\sum \mathbf{e_i}$$
$$\bar{\mathbf{q}}_{t+1} = \mathbf{e}\bar{\mathbf{q}}_t$$

We iterate until a matric on $e$ is satisfying (like its angle as measured by its real component) or after a fixed number of iteration.

## Helper functions and matrices

We introduce some helper matrices. 

- $\mathbf{R}_{b2f}\{\mathbf{q}\}$ is the body to fixed vector rotation matrix. It transforms vector in the body frame to the fixed world frame. It takes as parameter the attitude $q$.
- $\mathbf{R}_{f2b}\{\mathbf{q}\}$ is its inverse matrix (from fixed to body).
- $\mathbf{T}_{2a} = (0, 0, 1/m)^T$ is the scaling from thrust to acceleration (by dividing by the weight of the drone: $\mathbf{F} = m\mathbf{a} \Rightarrow \mathbf{a} = \mathbf{F}/m)$ and then multiplying by a unit vector $(0, 0, 1)$
- $$R2Q(\boldsymbol{\theta}) = (\cos(\| \boldsymbol{\theta} \| / 2), \sin(\| \boldsymbol{\theta} \| / 2) \frac{\boldsymbol{\theta}}{\| \boldsymbol{\theta} \|} )$$ is a function that convert from a local angle rotation to a local quaternion rotation. The definition of this function come from converting $\boldsymbol{\theta}$ to a body-axis angle, and then to a quaternion.
- $$Q2R(\mathbf{q}) = (q.i*s, q.j*s, q.k*s) $$ is its inverse function where $n = \arccos(q.w)*2$ and $s = n/\sin(n/2)$
- $\Delta t$ is the lapse of time between t and the next tick (t+1)

## Model 

The drone is submittied to the wind, the gravity and its own inertia.

- $\mathbf{w}$ the wind acceleration
- $\mathbf{a}$ the total acceleration
- $\mathbf{v}$ the velocity
- $\mathbf{p}$ the position
- $\boldsymbol{\omega}$ the angular velocity
- $\mathbf{q}$ the attitude

## Sensors

This work was in collaboration with the ASL Stanford lab for indoor POSE of drones. The sensors at disposition are:

- **Accelerometer**: It generates $\mathbf{a_A}$ a measurement of the total acceleration in the body frame referrential the drone is submitted to at a **high** sampling rate. The measurements model is: 
$$\mathbf{a_A}(t) = \mathbf{R}_{f2b}\{\mathbf{q}(t)\}\mathbf{a}(t) + \mathbf{a_A}^\epsilon$$ where the covariance matrix of the noise of the accelerometer is ${\mathbf{R}_{\mathbf{a_A}}}_{3 \times 3}$ and $$\mathbf{a_A}^\epsilon \sim \mathcal{N}(\mathbf{0}, \mathbf{R}_{\mathbf{a_A}})$$. 
- **Gyroscope**:It generates $\mathbf{\boldsymbol{\omega}_G}$ a measurement of the angular velocity in the body frame of the drone at the last timestep  at a **high** sampling rate. The measurement model is: 
$$\mathbf{\boldsymbol{\omega}_G}(t) = \boldsymbol{\omega} + \mathbf{\boldsymbol{\omega}_G}^\epsilon$$ where the covariance matrix of the noise of the accelerometer is ${\mathbf{R}_{\mathbf{\boldsymbol{\omega}_G}}}_{3 \times 3}$ and $$\mathbf{\boldsymbol{\omega}_G}^\epsilon_t \sim \mathcal{N}(\mathbf{0}, \mathbf{R}_{\mathbf{\boldsymbol{\omega}_G}})$$.
- **Position**: It generates $\mathbf{p_V}$ a measurement of the current positionat a **low** sampling rate. This is usually provided by a **Vicon** (for indoor), **GPS**, a **Tango** or any other position sensor. 
The measurement model is: 
$$\mathbf{p_V}(t) = \mathbf{p}(t) + \mathbf{p_V}^\epsilon$$ where the covariance matrix of the noise of the position is ${\mathbf{R}_{\mathbf{p_V}}}_{3 \times 3}$ and $$\mathbf{p_V}^\epsilon \sim \mathcal{N}(\mathbf{0}, \mathbf{R}_{\mathbf{p_V}})$$. 
- **Attitude**: It generates $\mathbf{q_V}$ a measurement of the current attitute. This is usually provided in addition to the position by a **Vicon** or a **Tango** at a **low** sampling rate or the **Magnemoter** at a **high** sampling rate if the environment permit it (no high magnetic interference nearby like iron contamination). 
The measurement model is: 
$$\mathbf{q_V}(t) = \mathbf{q}(t)*R2Q(\mathbf{q_V}^\epsilon)$$ where the covariance matrix of the noise of the attitude is ${\mathbf{R}_{\mathbf{q_V}}}_{3 \times 3}$ and $$\mathbf{q_V}^\epsilon \sim \mathcal{N}(\mathbf{0}, \mathbf{R}_{\mathbf{q_V}})$$. 


This work adapts itself easily to other sensors. Most  sensors available in the market are closely related to one of the 4 presented here. For instance an altimeter is a position estimator but for the z-axis only.

### Control inputs 

The control inputs at disposition are:

- **Thrust**: The current thrust in the direction of the orientation of the drone the motors should create. It is given as $t_C$ thrust (as a scalar) in the direction of the attitude.
- **Angular velocity**: The angular velocity the motors should create. It is given as $\mathbf{\boldsymbol{\omega}_C}$ angular velocity in the body frame.

Observations from the control input are not strictly speaking measurements but input of the state-transition model.


### Assumptions

We assume rigid body physics which is a good enough approximation for drones. 

We assume in this work that the wind is a constant field vector that evolve over time in a "smooth" fashion such that it makes sense to keep track of it.

We assume that since the biases of the sensor are known, the sensor have been calibrated and output measurements with no bias. Some EKF keep track of the sensor biases but this is a state augmentation that was not deemed worthwhile.

Finally, we also assume that the profile (surface on which the wind applies pressure on) of the drone against the wind is approximately the same for all attitude of the drone. This is definitely not true and this could be easily fixed by using an approximate and differentiable function to measure the profile of the drone in every direction.

### Model dynamic

- $\mathbf{w}(t+1) = \mathbf{w} + \mathbf{a}^\epsilon_t$ where $\mathbf{w}^\epsilon_t \sim \mathcal{N}(\mathbf{0}, \mathbf{Q}_{\mathbf{w}_t })$
- $\mathbf{a}(t+1) = \mathbf{R}_{b2f}\{\mathbf{q}(t+1)\}\mathbf{T}_{2a} {t_C}(t+1) + \mathbf{w}(t) + \mathbf{a}^\epsilon_t$ where $\mathbf{a}^\epsilon_t \sim \mathcal{N}(\mathbf{0}, \mathbf{Q}_{\mathbf{a}_t })$
- $\mathbf{v}(t+1) = \mathbf{v}(t) + \Delta t \mathbf{a}(t) + \mathbf{v}^\epsilon_t$ where $\mathbf{v}^\epsilon_t \sim \mathcal{N}(\mathbf{0}, \mathbf{Q}_{\mathbf{v}_t })$
- $\mathbf{p}(t+1) = \mathbf{p}(t) + \Delta t \mathbf{v}(t) + \mathbf{p}^\epsilon_t$ where $\mathbf{p}^\epsilon_t \sim \mathcal{N}(\mathbf{0}, \mathbf{Q}_{\mathbf{p}_t })$
- $\boldsymbol{\omega}(t+1) = \mathbf{\boldsymbol{\omega}_C}(t+1) + \mathbf{p}^\epsilon_t$ where $\mathbf{p}^\epsilon_t \sim \mathcal{N}(\mathbf{0}, \mathbf{Q}_{\mathbf{p}_t })$
- $\mathbf{q}(t+1) = \mathbf{q}(t)*R2Q(\Delta t \boldsymbol{ \omega(t) })$

Note that in our model, $\mathbf{q}(t+1)$ must be known. Fortunately, as we will see later, our RBPF is conditionned under the attitude so it is known.

## State

The time series of the variables of our dynamic model constitute a hidden markov chain. Indeed, the model is "memoryless" and depend only on the current state and of a sampled transition. 

States contain variables that enable us to keep track of some of those hidden variables which is our ultimate goal (for POSE $\mathbf{p}$ and $\mathbf{q}$). States at time $t$ are denoted by $\mathbf{x}_t$. Different filters require different state variables depending on their structure and assumptions. 

## Observation

Observations are revealed variables conditionned under the variables of our dynamic model. Our ultimate goal is to deduce the states from the observations. 

Observations contain the control input $\mathbf{u}$ and the measurements $\mathbf{z}$.

$$\mathbf{y}_t = (\mathbf{z}_t, \mathbf{u}_t)^T = ((\mathbf{a_A}_t, \mathbf{\boldsymbol{\omega}_G}_t, \mathbf{p_V}_t, \mathbf{q_V}_t), ({t_C}_t, \mathbf{\boldsymbol{\omega}_C}_t))^T$$


## Filtering and smoothing

**Smoothing** is the statistical task of finding the expectation of the state variable from the past history of observations and multiple observation variables ahead

$$\mathbb{E}[g(\mathbf{x}_{0:t}) | \mathbf{y}_{1:t+k}]$$

Which expand to,

$$\mathbb{E}[(\mathbf{p}_{0:t}, \mathbf{q}_{0:t}) | (\mathbf{z}_{1:t+k}, \mathbf{u}_{1:t+k})]$$

$k$ is a contant and the first observation is $y_1$

**Filtering** is a kind of smoothing where you only have at disposal the current observation variable ($k=0$)


# Complementary Filter

The complementary filter is the simplest of all and very common to retrieve the attitude on low-computing systems because of its low computational complexity. The gyroscope and accelerometer both provide a measurement that can help us to estimate the attitude. The gyroscope indeed gives us a noisy measurement of the angular velocity from which we can retrieve the new attitude from the past one by time integration: $\mathbf{q}_t = \mathbf{q}_{t-1}*R2Q(\Delta t \mathbf{\omega})$. This is commonly called "Dead reckoning"[^ded] and is prone to accumulation error, referred as drift. Indeed, like brownian motions, even if the process is unbiased, the variance grows with time. Reducing the noise cannot solve the issue entirely: even with extremely precise instruments, you are subject to floating point errors.

Fortunately, the accelerometer gives us a highly unprecise measurement of the orientation but is not subject to drift. Indeed, if not subject to other accelerations, the accelerometer measures the gravity field orientation. Since this field is oriented toward earth, it is possible to retrieve the current rotation from that field and by extension the attitude. However, in the case of a drone, it is subject to continuous and signifiant acceleration and vibration. Hence, the assumption that we retrieve the gravity field directly is wrong. We can solve this by substracting the acceleration deduced from the thrust control input. 

**TODO** pictures

The idea of the filter is to combine the precise short-term measurements of the gyroscope subject to drift with the "long-term" measurements of the accelerometer.

**TODO**: continue

# Kalman Filter

## Linearity

Kalman filters are optimal linear filters. Hence, Kalman filters are non optimal for our problem because our state has some non-linear components (attitude).Indeed, rotations and attitudes belong to $SO(3)$ which is not a vector space. Therefore, we cannot use vanilla kalman filters. However, understanding kalman filters give us a good understanding of Bayesian inference and the filters that we will present after use Kalman filters or are extensions of them to deal with non-linearity. One example of such extension is the extended kalman filter that we will present in the following section.

**TODO**: Kalman explanataion


## Extended Kalman Filters

EKF are linearized Kalman filters of the first order.

An EKF is semi-developped in the annex.

**TODO**: EKF explanations

## Unscented Kalman Filters

**TODO**: UKF explanations

# Particle Filter

Particle filters are computionally expensive and that is why their usage is not very popular currently for low-powered embedded systems.

Particle filters are monte carlo methods which in their general form ... **TODO**

**TODO**

## Resampling

When the number of effective particles is too low ($N/10$), we apply systematic resampling

# Rao-Blackwellized Particle Filter 

## Introduction

Compared to a plain PF, RPBF leverage the linearity of some components of the state by assuming our model gaussian conditionned on a latent variable: Given the attitude $q_t$, our model is linear. This is where RPBF shines: We use particle filtering to estimate our latent variable, the attitude, and we use the optimal kalman filter to estimate the state variable. 

This main inspiration from this approach is [@vernaza_rao-blackwellized_2006]. However, it differs by:

- adding the wind as a state augmentation
- adapt the filter to drones by taking into account that the system is too dynamic for assuming that the accelerometer simply output the gravity vector. This is solved by augmenting the state with the acceleration as shown later.
- not use measurements of the IMU as control inputs (this is usually used for wheeled vehicles because of the drift from the wheels) but have both control inputs and measurements.
- add an attitude sensor.

We introduce the latent variable $\boldsymbol{\theta}$

The latent variable $\boldsymbol{\theta}$ has for sole component the attitude: $$\boldsymbol{\theta} = (\mathbf{q})$$

$q_t$ is estimated from the product of the attitude of all particles $\mathbf{\theta^{(i)}} = \mathbf{q}^{(i)}_t$ as the "average" quaternion $\mathbf{q}_t = avgQuat(\mathbf{q}^n_t)$. $x^n$ designates the product of all n arbitrary particle. 

We use importance sampling ... **TODO**

The weight definition is:

$$w^{(i)}_t = \frac{p(\boldsymbol{\theta}^{(i)}_{0:t} | \mathbf{y}_{1:t})}{\pi(\boldsymbol{\theta}^{(i)}_{0:t} | \mathbf{y}_{1:t})}$$

From the definition, it is proovable that:

$$w^{(i)}_t \propto \frac{p(\mathbf{y}_t | \boldsymbol{\theta}^{(i)}_{0:t-1}, \mathbf{y}_{1:t-1})p(\boldsymbol{\theta}^{(i)}_t | \boldsymbol{\theta}^{(i)}_{t-1})}{\pi(\boldsymbol{\theta}^{(i)}_t | \boldsymbol{\theta}^{(i)}_{1:t-1}, \mathbf{y}_{1:t})} w^{(i)}_{t-1}$$

We choose the dynamic of the model as the importance distribution:

$$\pi(\boldsymbol{\theta}^{(i)}_t | \boldsymbol{\theta}^{(i)}_{1:t-1}, \mathbf{y}_{1:t}) = p(\boldsymbol{\theta}^{(i)}_t | \boldsymbol{\theta}^{(i)}_{t-1}) $$

Hence, 

$$w^{(i)}_t \propto p(\mathbf{y}_t | \boldsymbol{\theta}^{(i)}_{0:t-1}, \mathbf{y}_{1:t-1}) w^{(i)}_{t-1}$$

We then sum all $w^{(i)}_t$ to find the normalization constant and retrieve the actual $w^{(i)}_t$

## State 

$$\mathbf{x}_t = (\mathbf{w}_t, \mathbf{a}_t, \mathbf{v}_t, \mathbf{p}_t)^T$$

Initial state $\mathbf{x}_0 = (\mathbf{0}, \mathbf{0}, \mathbf{0})$

Initial covariance matrix $\mathbf{\Sigma}_{9 \times 9} = \epsilon \mathbf{I}_{9 \times 9}$

## Latent variable

$$\mathbf{q}^{(i)}_{t+1} = \mathbf{q}^{(i)}_t*R2Q({\Delta t} (\mathbf{\boldsymbol{\omega}_C}_t+\mathbf{\boldsymbol{\omega}_C}^\epsilon_t))$$

$\mathbf{\boldsymbol{\omega}_C}^\epsilon_t$ represents the error from the control input and is sampled from $\mathbf{\boldsymbol{\omega}_C}^\epsilon_t \sim \mathcal{N}(\mathbf{0}, \mathbf{R}_{\mathbf{\boldsymbol{\omega}_C}_t })$

Initial attitude $\mathbf{q_0}$ is sampled such that the drone pitch and roll are none (parralel to the ground) but the yaw is unknown and uniformly distributed.

Note that $\mathbf{q}(t+1)$ is known in the [model dynamic](#model-dynamic) because the model is conditionned under $\boldsymbol{\theta}^{(i)}_{t+1}$.

## Measurement model

1. Accelerometer: 
$$\mathbf{a_A}(t) = \mathbf{R}_{f2b}\{\mathbf{q}(t)\}\mathbf{a}(t) + \mathbf{a_A}^\epsilon_t$$ where $\mathbf{a_A}^\epsilon_t \sim \mathcal{N}(\mathbf{0}, \mathbf{R}_{\mathbf{a_A}_t })$
2. Gyroscope: 
$$\mathbf{\boldsymbol{\omega}_G}(t) = Q2R({\mathbf{q}^{(i)}(t-1)}^{-1}\mathbf{q}^{(i)}(t))/\Delta t + \mathbf{\boldsymbol{\omega}_G}^\epsilon(t)$$ where $\mathbf{\boldsymbol{\omega}_G}^\epsilon_t \sim \mathcal{N}(\mathbf{0}, \mathbf{R}_{\mathbf{\boldsymbol{\omega}_G}})$
3. Position: 
$$\mathbf{p_V}(t) = \mathbf{p}(t)^{(i)} + \mathbf{p_V}^\epsilon_t$$ where $\mathbf{p_V}^\epsilon_t \sim \mathcal{N}(\mathbf{0}, \mathbf{R}_{\mathbf{p_V}_t })$
4. Attitude:
$$\mathbf{q_V}(t) = \mathbf{q}(t)^{(i)}*R2Q(\mathbf{q_V}^\epsilon_t)$$ where $\mathbf{q_V}^\epsilon_t \sim \mathcal{N}(\mathbf{0}, \mathbf{R}_{\mathbf{q_V}_t })$


## Kalman prediction

The model dynamics define the following model, state-transition matrix $\mathbf{F}_t\{\boldsymbol{\theta}^{(i)}_t\}$, the control-input matrix $\mathbf{B}_t\{\boldsymbol{\theta}^{(i)}_t\}$, the process noise $\mathbf{w}_t\{\boldsymbol{\theta}^{(i)}_t\}$ for the Kalman filter and its covariance $\mathbf{Q}_t\{\boldsymbol{\theta}^{(i)}_t\}$

$$\mathbf{x}_t = \mathbf{F}_t\{\boldsymbol{\theta}^{(i)}_t\} \mathbf{x}_{t-1} + \mathbf{B}_t\{\boldsymbol{\theta}^{(i)}_t\} \mathbf{u}_t + \mathbf{w}_t\{\boldsymbol{\theta}^{(i)}_t\}$$

$$\mathbf{F}_t\{\boldsymbol{\theta}^{(i)}_t\}_{12 \times 12} = 
\left( \begin{array}{ccc}
\mathbf{I}_{3 \times 3} &  & & \\
\mathbf{I}_{3 \times 3} &  & & \\
& \Delta t~\mathbf{I}_{3 \times 3} & \mathbf{I}_{3 \times 3} & \\
& & \Delta t~\mathbf{I}_{3 \times 3} & \mathbf{I}_{3 \times 3}
\end{array} \right)$$

$$\mathbf{B}_t\{\boldsymbol{\theta}^{(i)}_t\}_{9 \times 5} = 
\left( \begin{array}{ccc}
\mathbf{0}_{3 \times 3} & \\
& \mathbf{R}_{b2f}\{\mathbf{q}^{(i)}_{t}\}\mathbf{T}_{2a} \\
& \\
&
\end{array} \right)$$

$$\mathbf{Q}_t\{\boldsymbol{\theta}^{(i)}_t\}_{12 \times 12} = 
\left( \begin{array}{cccc}
\mathbf{Q}_{\mathbf{w}_t } & & &\\
& \mathbf{Q}_{\mathbf{a}_t } & &\\
& & \mathbf{Q}_{\mathbf{v}_t }& \\
& & &\mathbf{Q}_{\mathbf{p}_t }
\end{array} \right)$$

$$\hat{\mathbf{x}}^{-(i)}_t = \mathbf{F}_t\{\boldsymbol{\theta}^{(i)}_t\} \mathbf{x}^{(i)}_{t-1} + \mathbf{B}_t\{\boldsymbol{\theta}^{(i)}_t\} \mathbf{u}_t $$
$$ \mathbf{\Sigma}^{-(i)}_t = \mathbf{F}_t\{\boldsymbol{\theta}^{(i)}_t\} \mathbf{\Sigma}^{-(i)}_{t-1}  (\mathbf{F}_t\{\boldsymbol{\theta}^{(i)}_t\})^T + \mathbf{Q}_t\{\boldsymbol{\theta}^{(i)}_t\}$$

## Kalman measurement update

The [measurement model](
#measurements-model-1) defines how to compute $p(\mathbf{y}_t | \boldsymbol{\theta}^{(i)}_{0:t-1}, \mathbf{y}_{1:t-_K1})$ 

In the [measurement model](
#measurements-model-1), (1, 3) define the observation matrix $\mathbf{H}_t\{\boldsymbol{\theta}^{(i)}_t\}$, the observation noise $\mathbf{v}_t\{\boldsymbol{\theta}^{(i)}_t\}$ and its covariance matrix $\mathbf{R}_t\{\boldsymbol{\theta}^{(i)}_t\}$ for the Kalman filter.

$$(\mathbf{a_A}_t, \mathbf{p_V}_t)^T  = \mathbf{H}_t\{\boldsymbol{\theta}^{(i)}_t\} (\mathbf{w}_t, \mathbf{a}_t, \mathbf{v}_t, \mathbf{p}_t)^T + \mathbf{v}_t\{\boldsymbol{\theta}^{(i)}_t\}$$	

$$\mathbf{H}_t\{\boldsymbol{\theta}^{(i)}_t\}_{6 \times 12} = 
\left( \begin{array}{ccc}
\mathbf{I}_{3 \times 3} & \mathbf{R}_{f2b}\{\mathbf{q}^{(i)}_{t}\} & & \mathbf{0}_{3 \times 3} \\
& & \mathbf{I}_{3 \times 3} & \\
\end{array} \right)$$


$$\mathbf{R}_t\{\boldsymbol{\theta}^{(i)}_t\}_{6 \times 1} = 
\left( \begin{array}{cc}
\mathbf{R}_{\mathbf{a_A}_t } & \\
& \mathbf{R}_{\mathbf{p_V}_t }
\end{array} \right)$$

### Kalman update

$$\mathbf{S} = \mathbf{H}_t\{\boldsymbol{\theta}^{(i)}_t\} \mathbf{\Sigma}^{-(i)}_t  (\mathbf{H}_t\{\boldsymbol{\theta}^{(i)}_t\})^T + \mathbf{R}_t\{\boldsymbol{\theta}^{(i)}_t\}$$
$$\hat{\mathbf{z}} = \mathbf{H}_t\{\boldsymbol{\theta}^{(i)}_t\}  \hat{\mathbf{x}}^{-(i)}_t$$
$$\mathbf{K} = \mathbf{\Sigma}^{-(i)}_t \mathbf{H}_t\{\boldsymbol{\theta}^{(i)}_t\}^T \mathbf{S}^{-1}$$
$$\mathbf{\Sigma}^{(i)}_t = \mathbf{\Sigma}^{-(i)}_t + \mathbf{K} \mathbf{S} \mathbf{K}^T$$
$$\hat{\mathbf{x}}^{(i)}_t = \hat{\mathbf{x}}^{-(i)}_t  + \mathbf{K}((\mathbf{a_A}_t, \mathbf{p_V}_t)^T - \hat{\mathbf{z}})$$
$$p(\mathbf{y}_t | \boldsymbol{\theta}^{(i)}_{0:t-1}, \mathbf{y}_{1:t-1}) = \mathcal{N}((\mathbf{a_A}_t, \mathbf{p_V}_t)^T; \hat{\mathbf{z}}_t, \mathbf{S})$$

### Asynchronous measurements

Our measurements from the Vicon and the accelerometer have different sampling rate so instead of doing full kalman update, we only apply a partial kalman update corresponding to the current type of measurement $\mathbf{z}_t$

For instance, we would apply the kalman update from the previous section but with: 

- for $\mathbf{a_A}_t$: 
$$\mathbf{H}_t\{\boldsymbol{\theta}^{(i)}_t\}_{3 \times 6} = (\mathbf{R}_{f2b}\{\mathbf{q}^{(i)}_{t}\} \mathbf{0}_{3 \times 3})$$
$$\mathbf{R}_t\{\boldsymbol{\theta}^{(i)}_t\}_{3 \times 3} = \mathbf{R}_{f2b}\{\mathbf{q}^{(i)}_{t}\}\mathbf{R}_{\mathbf{a_A}_t } $$
$$\mathbf{x}^{(i)}_t = \mathbf{H}_t\{\boldsymbol{\theta}^{(i)}_t\} \mathbf{x}^{(i)}_{t-1} + \mathbf{K}(\mathbf{a_A}_t - \hat{\mathbf{z}})$$
$$p(\mathbf{y}_t | \boldsymbol{\theta}^{(i)}_{0:t-1}, \mathbf{y}_{1:t-1}) = \mathcal{N}(\mathbf{a_A}_t; \hat{\mathbf{z}}_t, \mathbf{S})$$


- for $\mathbf{p_V}_t$: 
$$\mathbf{H}_t\{\boldsymbol{\theta}^{(i)}_t\}_{3 \times 6} = (\mathbf{0}_{3 \times 3} \mathbf{I}_{3 \times 3} )$$
$$\mathbf{R}_t\{\boldsymbol{\theta}^{(i)}_t\}_{3 \times 3} =  \mathbf{R}_{\mathbf{p_V}_t }$$
$$\mathbf{x}^{(i)}_t = \mathbf{H}_t\{\boldsymbol{\theta}^{(i)}_t\} \mathbf{x}^{(i)}_{t-1} + \mathbf{K}(\mathbf{p_V}_t - \hat{\mathbf{z}})$$
$$p(\mathbf{y}_t | \boldsymbol{\theta}^{(i)}_{0:t-1}, \mathbf{y}_{1:t-1}) = \mathcal{N}(\mathbf{p_V}_t; \hat{\mathbf{z}}_t, \mathbf{S})$$

## Other sources or reweighting

In the [measurement model](
#measurements-model), (2 and 4) define two other weight updates.

$$p(\mathbf{y}_t | \boldsymbol{\theta}^{(i)}_{0:t-1}, \mathbf{y}_{1:t-1}) = \mathcal{N}(\mathbf{\boldsymbol{\omega}_G}_t; Q2R({\mathbf{q}^{(i)}_{t-1}}^{-1}\mathbf{q}^{(i)}_t)/\Delta t,~ \mathbf{R}_{\mathbf{\boldsymbol{\omega}_G}})$$

$$p(\mathbf{y}_t | \boldsymbol{\theta}^{(i)}_{0:t-1}, \mathbf{y}_{1:t-1}) = \mathcal{N}(Q2R({\mathbf{q}^{(i)}}^{-1}\mathbf{q_V}_t);~ 0 ,~ \mathbf{R}_{\mathbf{q_V}})$$


## Algorithm summary

1. Initiate $N$ particles with $\mathbf{x}_0$, $\mathbf{q}_0 ~ \sim p(\mathbf{q}_0)$, $\mathbf{\Sigma}_0$ and $w = 1/N$ 
2. While new sensor measurements $(\mathbf{z}_t, \mathbf{u}_t)$ 
   - foreach $N$ particles $(i)$:
       1. sample new latent variable $\boldsymbol{\theta_t}$ from $\mathbf{u}_t$
       2. Depending on the type of measurement:
     	   - **Accelerometer**:
		   Partial kalman update with: 
		   $$\mathbf{H}_t\{\boldsymbol{\theta}^{(i)}_t\}_{3 \times 6} = (\mathbf{R}_{f2b}\{\mathbf{q}^{(i)}_{t}\} ~~~~ \mathbf{0}_{3 \times 3})$$
		   $$\mathbf{R}_t\{\boldsymbol{\theta}^{(i)}_t\}_{3 \times 3} = \mathbf{R}_{f2b}\{\mathbf{q}^{(i)}_{t}\}\mathbf{R}_{\mathbf{a_A}_t } $$
		   $$\mathbf{x}^{(i)}_t = \mathbf{H}_t\{\boldsymbol{\theta}^{(i)}_t\} \mathbf{x}^{(i)}_{t-1} + \mathbf{K}(\mathbf{a_A}_t - \hat{\mathbf{z}})$$
		   $$p(\mathbf{y}_t | \boldsymbol{\theta}^{(i)}_{0:t-1}, \mathbf{y}_{1:t-1}) = \mathcal{N}(\mathbf{a_A}_t; \hat{\mathbf{z}}_t, \mathbf{S})$$
           - **Gyroscope**: 
		   $$p(\mathbf{y}_t | \boldsymbol{\theta}^{(i)}_{0:t-1}, \mathbf{y}_{1:t-1}) = \mathcal{N}(\mathbf{\boldsymbol{\omega}_G}_t; (\mathbf{q}^{(i)}_t - \mathbf{q}^{(i)}_{t-1})/\Delta t,~ \mathbf{R}_{\mathbf{\boldsymbol{\omega}_G}_t})$$
     	   - **Vicon**: 
		   Partial kalman update with: 
		   $$\mathbf{H}_t\{\boldsymbol{\theta}^{(i)}_t\}_{3 \times 6} = (\mathbf{0}_{3 \times 3} ~~~~ \mathbf{I}_{3 \times 3} )$$
		   $$\mathbf{R}_t\{\boldsymbol{\theta}^{(i)}_t\}_{3 \times 3} =  \mathbf{R}_{\mathbf{p_V}_t }$$
		   $$\mathbf{x}^{(i)}_t = \mathbf{H}_t\{\boldsymbol{\theta}^{(i)}_t\} \mathbf{x}^{(i)}_{t-1} + \mathbf{K}(\mathbf{p_V}_t - \hat{\mathbf{z}})$$
		   $$p(\mathbf{y}_t | \boldsymbol{\theta}^{(i)}_{0:t-1}, \mathbf{y}_{1:t-1})^- = \mathcal{N}(\mathbf{p_V}_t; \hat{\mathbf{z}}_t, \mathbf{S})$$
		   $$p(\mathbf{y}_t | \boldsymbol{\theta}^{(i)}_{0:t-1}, \mathbf{y}_{1:t-1}) = \mathcal{N}(\mathbf{q_V}_t; \mathbf{q}^{(i)}_t,~ \mathbf{R}_{\mathbf{q_V}_t }) p(\mathbf{y}_t | \boldsymbol{\theta}^{(i)}_{0:t-1}, \mathbf{y}_{1:t-1})^-$$
		   
      3. Update $w^{(i)}_t$: $w^{(i)}_t = p(\mathbf{y}_t | \boldsymbol{\theta}^{(i)}_{0:t-1}, \mathbf{y}_{1:t-1}) w^{(i)}_{t-1}$	  
  - Normalize all $w^{(i)}$ by scalaing by $1/(\sum w^{(i)})$ such that $\sum w^{(i)}= 1$
  - Compute $\mathbf{p}_t$ and $\mathbf{q}_t$ as the expectation from the distribution approximated by the N particles.
  - Resample if the number of effective particle is too low


# Appendix

## Semi developped Kalman filter

## State 

For the EKF, we are gonna use the following state:

$$\mathbf{x}_t = (\mathbf{w}_t, \mathbf{a}_t, \mathbf{v}_t, \mathbf{p}_t, \boldsymbol{\omega}_t, \mathbf{q}_t)^T$$

Initial state $\mathbf{x}_0$ at $(\mathbf{0}, \mathbf{0}, \mathbf{0}, \mathbf{0}, \mathbf{0}, (1, 0, 0, 0))$

## Measurements model

1. Accelerometer: 
$$\mathbf{a_A}(t) = \mathbf{R}_{f2b}\{\mathbf{q}(t)\}\mathbf{a}(t) + \mathbf{w}(t) + \mathbf{a_A}^\epsilon_t$$ where $\mathbf{a_A}^\epsilon_t \sim \mathcal{N}(\mathbf{0}, \mathbf{R}_{\mathbf{a_A}_t })$
2. Gyroscope: 
$$\mathbf{\boldsymbol{\omega}_G}(t) = \mathbf{\boldsymbol{\omega}_G} + \mathbf{\boldsymbol{\omega}_G}^\epsilon(t)$$ where $\mathbf{\boldsymbol{\omega}_G}^\epsilon_t \sim \mathcal{N}(\mathbf{0}, \mathbf{R}_{\mathbf{\boldsymbol{\omega}_G}})$
3. Position: 
$$\mathbf{p_V}(t) = \mathbf{p}(t)^{(i)} + \mathbf{p_V}^\epsilon_t$$ where $\mathbf{p_V}^\epsilon_t \sim \mathcal{N}(\mathbf{0}, \mathbf{R}_{\mathbf{p_V}_t })$
4. Attitude:
$$\mathbf{q_V}(t) = \mathbf{q}(t)^{(i)}*R2Q(\mathbf{q_V}^\epsilon_t)$$ where $\mathbf{q_V}^\epsilon_t \sim \mathcal{N}(\mathbf{0}, \mathbf{R}_{\mathbf{q_V}_t })$


## Kalman prediction

The model dynamic defines the following model, state-transition function $f(\mathbf{x}, \mathbf{u})$ and process noise $\mathbf{w}$ with covariance matrix $\mathbf{Q}$

$$\mathbf{x}_t = f(\mathbf{x}_{t-1}, \mathbf{u}_t) + \mathbf{w}_t$$

$$f((\mathbf{w}, \mathbf{a}, \mathbf{v}, \mathbf{p}, \boldsymbol{\omega}, \mathbf{q}), (t_C, \mathbf{\boldsymbol{\omega}_C})) = \left( \begin{array}{c}
\mathbf{w}\\
\mathbf{R}_{b2f}\{\mathbf{q}\}\mathbf{T}_{2a} {t_C} + \mathbf{w} \\
\mathbf{v} + \Delta t \mathbf{a} \\
\mathbf{p} + \Delta t \mathbf{v} \\
\mathbf{\boldsymbol{\omega}_C} \\
\mathbf{q}*R2Q({\Delta t} \boldsymbol{\omega})
\end{array} \right)$$

Now, we need to derive the jacobian of $f$. It is quite trivial except for the partial derivatives of $q$.
We will use sagemath to retrieve the 28 interesting different partial derivatives of $q$ as described in the appendix A.


$${\mathbf{F}_t}_{19 \times 19} = \left . \frac{\partial f}{\partial \mathbf{x} } \right \vert _{\hat{\mathbf{x}}_{t-1},\mathbf{u}_{t-1}} = \left( \begin{array}{ccccc}
\mathbf{I}_{3 \times 3} & & & & & \\
\mathbf{I}_{3 \times 3} & & & & & \\
& \Delta t~\mathbf{I}_{3 \times 3} & \mathbf{I}_{3 \times 3} & & & \\
& & \Delta t~\mathbf{I}_{3 \times 3} & \mathbf{I}_{3 \times 3} & & \\
& & & & \mathbf{I}_{3 \times 3} & \\
& & & & & \mathbf{F_q}_t
\end{array} \right)$$

where $\mathbf{F_q}_t$ is defined by the script sagemath in the appendix. 

$$\hat{\mathbf{x}}^{-(i)}_t = f(\mathbf{x}^{(i)}_{t-1}, \mathbf{u}_t)$$
$$\mathbf{\Sigma}^{-(i)}_t = \mathbf{F}_{t-1} \mathbf{\Sigma}^{-(i)}_{t-1}  \mathbf{F}_{t-1}^T + \mathbf{Q}_t$$


### Kalman measurements update

$$\mathbf{z}_t = h(\mathbf{x}_t) + \mathbf{v}_t$$

The [measurement model](
#measurements-model) defines $h(\mathbf{x})$

$$\left( \begin{array}{c}
\mathbf{a_A}\\
\mathbf{\boldsymbol{\omega}_G}\\
\mathbf{p_V}\\
\mathbf{q_V}\\
\end{array} \right) = h((\mathbf{w}, \mathbf{a}, \mathbf{v}, \mathbf{p}, \boldsymbol{\omega}, \mathbf{q})) = \left( \begin{array}{c}
\mathbf{R}_{f2b}\{\mathbf{q}\}\mathbf{a} + \mathbf{w}\\
\boldsymbol{\omega}\\
\mathbf{p}\\
\mathbf{q}\\
\end{array} \right)$$

The only complex partial derivatives to calculate are the one of the acceleration, because they have to be rotated first. Once again, we use sagemath:
$\mathbf{H_a}$ is defined by the script in the appendix B.

$${\mathbf{H}_t}_{16 \times 16} = \left . \frac{\partial h}{\partial \mathbf{x} } \right \vert _{\hat{\mathbf{x}}_{t}} = \left( \begin{array}{ccccc}
\mathbf{H_a}_{3 \times 3} & & & & \\
& \mathbf{I}_{3 \times 3} & & & \\
& & \mathbf{I}_{3 \times 3} & & \\
& & & \mathbf{I}_{3 \times 3} &  \\
& & & & \mathbf{I}_{4 \times 4}  \\
\end{array} \right)$$

$${\mathbf{R}_t}_{13 \times 13} = 
\left( \begin{array}{cccc}
\mathbf{R}_{\mathbf{a_A}_t} & & & \\
& \mathbf{R}_{\mathbf{\boldsymbol{\omega}_G}} & & \\
& & \mathbf{R}_{\mathbf{p_V}} & \\
& & & ???\\
\end{array} \right)$$

**TODO**: Use (Shibani, 2011) to replace ???

### Kalman update

$$\mathbf{S} = \mathbf{H}_t \mathbf{\Sigma}^{-}_t \mathbf{H}_t^T + \mathbf{R}_t$$
$$\hat{\mathbf{z}} = h(\hat{\mathbf{x}}^{-}_t)$$
$$\mathbf{K} = \mathbf{\Sigma}^{-}_t \mathbf{H}_t^T \mathbf{S}^{-1}$$
$$\mathbf{\Sigma}_t = \mathbf{\Sigma}^-_t + \mathbf{K} \mathbf{S} \mathbf{K}^T$$
$$\hat{\mathbf{x}}_t = \hat{\mathbf{x}}^{-}_t + \mathbf{K}(\mathbf{z}_t - \hat{\mathbf{z}})$$



## F partial derivatives 

```python
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

v = vector(nq.coefficient_tuple())

for sym in [wx, wy, wz, q0, q1, q2, q3]:
    d = diff(v, sym)
    exps = map(lambda x: x.canonicalize_radical().full_simplify(), d)
    for i, e in enumerate(exps):
        print(sym, i, e) 
		
# Here are the results

(wx, 0, -1/2*((dt*q1*wx^2 + dt*q2*wx*wy + dt*q3*wx*wz)*sqrt(wx^2 + wy^2 + wz^2)*cos(1/2*sqrt(wx^2 + wy^2 + wz^2)*dt) + (dt*q0*wx^3 - 2*q2*wx*wy + (dt*q0*wx + 2*q1)*wy^2 - 2*q3*wx*wz + (dt*q0*wx + 2*q1)*wz^2)*sin(1/2*sqrt(wx^2 + wy^2 + wz^2)*dt))/(wx^2 + wy^2 + wz^2)^(3/2))
(wx, 1, -1/2*((dt*q1*wx^3 - 2*q3*wx*wy + (dt*q1*wx - 2*q0)*wy^2 + 2*q2*wx*wz + (dt*q1*wx - 2*q0)*wz^2)*sqrt(wx^2 + wy^2 + wz^2)*sin(1/2*sqrt(wx^2 + wy^2 + wz^2)*dt) - (dt*q0*wx^4 - dt*q3*wx^3*wy + dt*q0*wx^2*wy^2 - dt*q3*wx*wy^3 + dt*q2*wx*wz^3 + (dt*q0*wx^2 - dt*q3*wx*wy)*wz^2 + (dt*q2*wx^3 + dt*q2*wx*wy^2)*wz)*cos(1/2*sqrt(wx^2 + wy^2 + wz^2)*dt))/(wx^4 + 2*wx^2*wy^2 + wy^4 + wz^4 + 2*(wx^2 + wy^2)*wz^2))
(wx, 2, 1/2*((dt*q3*wx^2 + dt*q0*wx*wy - dt*q1*wx*wz)*sqrt(wx^2 + wy^2 + wz^2)*cos(1/2*sqrt(wx^2 + wy^2 + wz^2)*dt) - (dt*q2*wx^3 + 2*q0*wx*wy + (dt*q2*wx - 2*q3)*wy^2 - 2*q1*wx*wz + (dt*q2*wx - 2*q3)*wz^2)*sin(1/2*sqrt(wx^2 + wy^2 + wz^2)*dt))/(wx^2 + wy^2 + wz^2)^(3/2))
(wx, 3, -1/2*((dt*q3*wx^3 + 2*q1*wx*wy + (dt*q3*wx + 2*q2)*wy^2 + 2*q0*wx*wz + (dt*q3*wx + 2*q2)*wz^2)*sqrt(wx^2 + wy^2 + wz^2)*sin(1/2*sqrt(wx^2 + wy^2 + wz^2)*dt) + (dt*q2*wx^4 - dt*q1*wx^3*wy + dt*q2*wx^2*wy^2 - dt*q1*wx*wy^3 - dt*q0*wx*wz^3 + (dt*q2*wx^2 - dt*q1*wx*wy)*wz^2 - (dt*q0*wx^3 + dt*q0*wx*wy^2)*wz)*cos(1/2*sqrt(wx^2 + wy^2 + wz^2)*dt))/(wx^4 + 2*wx^2*wy^2 + wy^4 + wz^4 + 2*(wx^2 + wy^2)*wz^2))
(wy, 0, -1/2*((dt*q1*wx*wy + dt*q2*wy^2 + dt*q3*wy*wz)*sqrt(wx^2 + wy^2 + wz^2)*cos(1/2*sqrt(wx^2 + wy^2 + wz^2)*dt) + (dt*q0*wy^3 + 2*q2*wx^2 - 2*q3*wy*wz + (dt*q0*wy + 2*q2)*wz^2 + (dt*q0*wx^2 - 2*q1*wx)*wy)*sin(1/2*sqrt(wx^2 + wy^2 + wz^2)*dt))/(wx^2 + wy^2 + wz^2)^(3/2))
(wy, 1, -1/2*((dt*q1*wy^3 + 2*q3*wx^2 + 2*q2*wy*wz + (dt*q1*wy + 2*q3)*wz^2 + (dt*q1*wx^2 + 2*q0*wx)*wy)*sqrt(wx^2 + wy^2 + wz^2)*sin(1/2*sqrt(wx^2 + wy^2 + wz^2)*dt) - (dt*q0*wx^3*wy - dt*q3*wx^2*wy^2 + dt*q0*wx*wy^3 - dt*q3*wy^4 + dt*q2*wy*wz^3 + (dt*q0*wx*wy - dt*q3*wy^2)*wz^2 + (dt*q2*wx^2*wy + dt*q2*wy^3)*wz)*cos(1/2*sqrt(wx^2 + wy^2 + wz^2)*dt))/(wx^4 + 2*wx^2*wy^2 + wy^4 + wz^4 + 2*(wx^2 + wy^2)*wz^2))
(wy, 2, 1/2*((dt*q3*wx*wy + dt*q0*wy^2 - dt*q1*wy*wz)*sqrt(wx^2 + wy^2 + wz^2)*cos(1/2*sqrt(wx^2 + wy^2 + wz^2)*dt) - (dt*q2*wy^3 - 2*q0*wx^2 - 2*q1*wy*wz + (dt*q2*wy - 2*q0)*wz^2 + (dt*q2*wx^2 + 2*q3*wx)*wy)*sin(1/2*sqrt(wx^2 + wy^2 + wz^2)*dt))/(wx^2 + wy^2 + wz^2)^(3/2))
(wy, 3, -1/2*((dt*q3*wy^3 - 2*q1*wx^2 + 2*q0*wy*wz + (dt*q3*wy - 2*q1)*wz^2 + (dt*q3*wx^2 - 2*q2*wx)*wy)*sqrt(wx^2 + wy^2 + wz^2)*sin(1/2*sqrt(wx^2 + wy^2 + wz^2)*dt) + (dt*q2*wx^3*wy - dt*q1*wx^2*wy^2 + dt*q2*wx*wy^3 - dt*q1*wy^4 - dt*q0*wy*wz^3 + (dt*q2*wx*wy - dt*q1*wy^2)*wz^2 - (dt*q0*wx^2*wy + dt*q0*wy^3)*wz)*cos(1/2*sqrt(wx^2 + wy^2 + wz^2)*dt))/(wx^4 + 2*wx^2*wy^2 + wy^4 + wz^4 + 2*(wx^2 + wy^2)*wz^2))
(wz, 0, -1/2*((dt*q3*wz^2 + (dt*q1*wx + dt*q2*wy)*wz)*sqrt(wx^2 + wy^2 + wz^2)*cos(1/2*sqrt(wx^2 + wy^2 + wz^2)*dt) + (dt*q0*wz^3 + 2*q3*wx^2 + 2*q3*wy^2 + (dt*q0*wx^2 + dt*q0*wy^2 - 2*q1*wx - 2*q2*wy)*wz)*sin(1/2*sqrt(wx^2 + wy^2 + wz^2)*dt))/(wx^2 + wy^2 + wz^2)^(3/2))
(wz, 1, -1/2*((dt*q1*wz^3 - 2*q2*wx^2 - 2*q2*wy^2 + (dt*q1*wx^2 + dt*q1*wy^2 + 2*q0*wx - 2*q3*wy)*wz)*sqrt(wx^2 + wy^2 + wz^2)*sin(1/2*sqrt(wx^2 + wy^2 + wz^2)*dt) - (dt*q2*wz^4 + (dt*q0*wx - dt*q3*wy)*wz^3 + (dt*q2*wx^2 + dt*q2*wy^2)*wz^2 + (dt*q0*wx^3 - dt*q3*wx^2*wy + dt*q0*wx*wy^2 - dt*q3*wy^3)*wz)*cos(1/2*sqrt(wx^2 + wy^2 + wz^2)*dt))/(wx^4 + 2*wx^2*wy^2 + wy^4 + wz^4 + 2*(wx^2 + wy^2)*wz^2))
(wz, 2, -1/2*((dt*q1*wz^2 - (dt*q3*wx + dt*q0*wy)*wz)*sqrt(wx^2 + wy^2 + wz^2)*cos(1/2*sqrt(wx^2 + wy^2 + wz^2)*dt) + (dt*q2*wz^3 + 2*q1*wx^2 + 2*q1*wy^2 + (dt*q2*wx^2 + dt*q2*wy^2 + 2*q3*wx + 2*q0*wy)*wz)*sin(1/2*sqrt(wx^2 + wy^2 + wz^2)*dt))/(wx^2 + wy^2 + wz^2)^(3/2))
(wz, 3, -1/2*((dt*q3*wz^3 - 2*q0*wx^2 - 2*q0*wy^2 + (dt*q3*wx^2 + dt*q3*wy^2 - 2*q2*wx + 2*q1*wy)*wz)*sqrt(wx^2 + wy^2 + wz^2)*sin(1/2*sqrt(wx^2 + wy^2 + wz^2)*dt) - (dt*q0*wz^4 - (dt*q2*wx - dt*q1*wy)*wz^3 + (dt*q0*wx^2 + dt*q0*wy^2)*wz^2 - (dt*q2*wx^3 - dt*q1*wx^2*wy + dt*q2*wx*wy^2 - dt*q1*wy^3)*wz)*cos(1/2*sqrt(wx^2 + wy^2 + wz^2)*dt))/(wx^4 + 2*wx^2*wy^2 + wy^4 + wz^4 + 2*(wx^2 + wy^2)*wz^2))
(q0, 0, cos(1/2*sqrt(wx^2 + wy^2 + wz^2)*dt))
(q0, 1, wx*sin(1/2*sqrt(wx^2 + wy^2 + wz^2)*dt)/sqrt(wx^2 + wy^2 + wz^2))
(q0, 2, wy*sin(1/2*sqrt(wx^2 + wy^2 + wz^2)*dt)/sqrt(wx^2 + wy^2 + wz^2))
(q0, 3, wz*sin(1/2*sqrt(wx^2 + wy^2 + wz^2)*dt)/sqrt(wx^2 + wy^2 + wz^2))
(q1, 0, -wx*sin(1/2*sqrt(wx^2 + wy^2 + wz^2)*dt)/sqrt(wx^2 + wy^2 + wz^2))
(q1, 1, cos(1/2*sqrt(wx^2 + wy^2 + wz^2)*dt))
(q1, 2, -wz*sin(1/2*sqrt(wx^2 + wy^2 + wz^2)*dt)/sqrt(wx^2 + wy^2 + wz^2))
(q1, 3, wy*sin(1/2*sqrt(wx^2 + wy^2 + wz^2)*dt)/sqrt(wx^2 + wy^2 + wz^2))
(q2, 0, -wy*sin(1/2*sqrt(wx^2 + wy^2 + wz^2)*dt)/sqrt(wx^2 + wy^2 + wz^2))
(q2, 1, wz*sin(1/2*sqrt(wx^2 + wy^2 + wz^2)*dt)/sqrt(wx^2 + wy^2 + wz^2))
(q2, 2, cos(1/2*sqrt(wx^2 + wy^2 + wz^2)*dt))
(q2, 3, -wx*sin(1/2*sqrt(wx^2 + wy^2 + wz^2)*dt)/sqrt(wx^2 + wy^2 + wz^2))
(q3, 0, -wz*sin(1/2*sqrt(wx^2 + wy^2 + wz^2)*dt)/sqrt(wx^2 + wy^2 + wz^2))
(q3, 1, -wy*sin(1/2*sqrt(wx^2 + wy^2 + wz^2)*dt)/sqrt(wx^2 + wy^2 + wz^2))
(q3, 2, wx*sin(1/2*sqrt(wx^2 + wy^2 + wz^2)*dt)/sqrt(wx^2 + wy^2 + wz^2))
(q3, 3, cos(1/2*sqrt(wx^2 + wy^2 + wz^2)*dt))
```

## H partial derivatives


```python
Q.<i,j,k> = QuaternionAlgebra(SR, -1, -1)

var('q0, q1, q2, q3')
var('ax, ay, az')

q = q0 + q1*i + q2*j + q3*k

#vector to quaternion
v = ax*i + ay*j + az*k

#Do a rotation of the vector v
nq = q*v*q.conjugate()

v = vector(nq.coefficient_tuple())
vs = map(lambda x: x.canonicalize_radical().full_simplify(), v)

for sym in [ax, ay, az, q0, q1, q2, q3]:
    d = diff(v, sym)
    exps = map(lambda x: x.canonicalize_radical().full_simplify(), d)[1:]
    for i, e in enumerate(exps):
        print(sym, i, e) 
		
#Here are the results
(ax, 0, q0^2 + q1^2 - q2^2 - q3^2)
(ax, 1, 2*q1*q2 + 2*q0*q3)
(ax, 2, -2*q0*q2 + 2*q1*q3)
(ay, 0, 2*q1*q2 - 2*q0*q3)
(ay, 1, q0^2 - q1^2 + q2^2 - q3^2)
(ay, 2, 2*q0*q1 + 2*q2*q3)
(az, 0, 2*q0*q2 + 2*q1*q3)
(az, 1, -2*q0*q1 + 2*q2*q3)
(az, 2, q0^2 - q1^2 - q2^2 + q3^2)
(q0, 0, 2*ax*q0 + 2*az*q2 - 2*ay*q3)
(q0, 1, 2*ay*q0 - 2*az*q1 + 2*ax*q3)
(q0, 2, 2*az*q0 + 2*ay*q1 - 2*ax*q2)
(q1, 0, 2*ax*q1 + 2*ay*q2 + 2*az*q3)
(q1, 1, -2*az*q0 - 2*ay*q1 + 2*ax*q2)
(q1, 2, 2*ay*q0 - 2*az*q1 + 2*ax*q3)
(q2, 0, 2*az*q0 + 2*ay*q1 - 2*ax*q2)
(q2, 1, 2*ax*q1 + 2*ay*q2 + 2*az*q3)
(q2, 2, -2*ax*q0 - 2*az*q2 + 2*ay*q3)
(q3, 0, -2*ay*q0 + 2*az*q1 - 2*ax*q3)
(q3, 1, 2*ax*q0 + 2*az*q2 - 2*ay*q3)
(q3, 2, 2*ax*q1 + 2*ay*q2 + 2*az*q3)
```

# References

[^ded]: The etymology for "Dead reckoning" comes from the mariners of the XVIIth century that used to calculate the position of the vessel with log book. The interpretation of "dead" is subject to debate. Some argue that it is a mispelling of "ded" as in "deduced". Others argue that it should be read by its old meaning: *absolute*.
