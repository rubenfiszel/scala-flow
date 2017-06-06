---
title: POSE estimation through Rao-Blackwellized Particle filter.
author: Ruben Fiszel
date: \today
---

# Introduction

We will introduce here several filter for POSE estimation for highly dynamic objects and in particular drones. The order is from the most conceptually simple, to the most complex. The Rao-Blackwellized Particle filter can be viewed as a superset of many other filters, where those filters are just instances of a particle filter with only one particle.

## Notes on notation and conventions

The referential by default is the fixed world frame. 

- $\mathbf{x}$ designates a vector
- $x_t$ is the random variable of x at time t
- $x_{t1:t2}$ is the product of the random variable of x between t1 included and t2 included
- $x^{(i)}$ designates the random variable x of the arbitrary particle i

## POSE 

POSE is the task of determining the position and orientation of an object through time. It is a subroutine of SLAM (Software Localization And Mapping). We can formelize the problem as:

At each timestep, find the best expectation of a function of the hidden variable state (position and orientation), from their initial distribution and observable random variables (such as sensor measurements).

- The state $\mathbf{x}$
- The function $g(\mathbf{x})$ such that $g(\mathbf{x}_t) = (\mathbf{p}_t, \mathbf{q}_t)$ where $\mathbf{p}$ is the position and $\mathbf{q}$ is the attitude as a quaternion.
- The observable variable $\mathbf{y}$ composed of the sensor measurements $\mathbf{z}$ and the control input $\mathbf{u}$

The algorithm inputs are:

- The distribution of initial position $\mathbf{p}_0$ and orientation $\mathbf{q}_0$
- control inputs $\mathbf{u}_t$ (the command sent to the flight controller)
- sensor measurements $\mathbf{z}_t$ coming from different sensors with different sampling rate
- information about the sensors (sensor measurements biases and matrix of covariance) 

## Sensors

This work was in collaboration with the ASL Stanford lab for indoor POSE of drones. The sensors at disposition are:

- **Accelerometer**: the total acceleration in the body frame referrential the drone is submitted to at a **high** sampling rate.
- **Gyroscope**: the angular velocity of the drone at the last timestep  at a **high** sampling rate.
- **Vicon** or **Tango**: an estimate of the current position and attitute at a **low** sampling rate.

The control inputs at disposition are:

- **Thrust**: The current thrust in the direction of the orientation of the drone the motors should create.
- **Angular velocity**: The angular velocity the motors should create.

This work adapts itself easily to other sensors but we will focus here only on those 3.

## Quaternions

Quaternions are extensions of complex numbers but with 3 imaginary parts. They can be used to represent orientation, also referred to as attitude. Quaternions algebra make rotation composition simple and quaternions avoid the issue of gimbal lock. In all filters presented, they will be used to represent the attitude.

## Filtering and smoothing

**Smoothing** is the statistical task of finding the expectation of the state variable from multiple observation variable ahead. 

$$\mathbb{E}[g(\mathbf{x}_{0:t}) | \mathbf{y}_{1:t+k}]$$

Which expand to,

$$\mathbb{E}[(\mathbf{p}_{0:t}, \mathbf{q}_{0:t}) | (\mathbf{z}_{1:t+k}, \mathbf{u}_{1:t+k})]$$

$k$ is a contant and the first observation is $y_1$

**Filtering** is a kind of smoothing where you only have at disposal the current observation variable ($k=0$)

# Complementary Filter


# Kalman Filter

Kalman filters are optimal linear filters.

**TODO**

## Linearity

Kalman filters are non optimal for our problem because our state has some non-linear components (attitude).

Indeed, rotations belong to $SO(3)$. It can be shown intuitively that they do not belong to a vector space because the sum of two unit quaternions is not a unit quaternion (not closed under addition).
**TODO**

## Extended Kalman Filters

EKF are linearized Kalman filters of the first order.

**TODO**

## Unscented Kalman Filters

**TODO**

# Particle Filter

Particle filters are computionally expensive and that is why their usage is not very popular currently for low-powered embedded systems.

Particle filters are monte carlo methods which in their general form ... **TODO**

**TODO**

## Resampling

When the number of effective particles is too low ($N/10$), we apply systematic resampling

# Rao-Blackwellized Particle Filter 

## Introduction

Compared to a plain PF, RPBF leverage the linearity of some components of the state by assuming our model gaussian conditionned on a latent variable: Given the attitude $q_t$, our model is linear. This is where RPBF shines: We use particle filtering to estimate our latent variable, the attitude, and we use the optimal kalman filter to estimate the state variable.

We introduce the latent variable $\boldsymbol{\theta}$

The latent variable $\boldsymbol{\theta}$ has for sole component the attitude: $$\boldsymbol{\theta} = (\mathbf{q})$$

$q_t$ is estimated from the product of the attitude of all particles $\mathbf{\theta^{(i)}} = \mathbf{q}^{(i)}_t$ as the "average" quaternion $\mathbf{q}_t = avgQuat(\mathbf{q}^n_t)$. $x^n$ designates the product of all n arbitrary particle. The average quaternion is not simply the average of its components ... **TODO**

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

$$\mathbf{x}_t = (\mathbf{a}_t, \mathbf{v}_t, \mathbf{p}_t)^T$$

- $\mathbf{a}$ acceleration
- $\mathbf{v}$ velocity
- $\mathbf{p}$ position

Initial position $\mathbf{p_0}$ at (0, 0, 0)

## Observations

$$\mathbf{y}_t = (\mathbf{z}_t, \mathbf{u}_t)^T = ((\mathbf{a_A}_t, \mathbf{\boldsymbol{\omega}_G}_t, \mathbf{p_V}_t, \mathbf{q_V}_t), ({t_C}_t, \mathbf{\boldsymbol{\omega}_C}_t))^T$$

### Measurements

- $\mathbf{a_A}$ acceleration from the accelerometer in the body frame
- $\mathbf{\boldsymbol{\omega}_G}$ angular velocity from the gyroscope in the body frame
- $\mathbf{p_V}$ position from the vicon
- $\mathbf{q_V}$ attitude from the vicon

### Control Inputs

- ${t_C}$ thrust (as a scalar) in the direction of the attitude from the control input. 
- $\mathbf{\boldsymbol{\omega}_C}$ angular velocity in the body frame from the control input

Observations from the control input are not strictly speaking measurements but input of the state-transition model

## Helper Matrices 

We introduce some helper matrices. 

- $\mathbf{R}_{b2f}\{\mathbf{q}\}$ is the body to fixed vector rotation matrix. It transforms vector in the body frame to the fixed world frame. It takes as parameter the attitude $q$.
- $\mathbf{R}_{f2b}\{\mathbf{q}\}$ is its inverse matrix (from fixed to body).
- $\mathbf{T}_{2a} = (0, 0, 1/m)^T$ is the scaling from thrust to acceleration (by dividing by the weight of the drone: $\mathbf{F} = m\mathbf{a} \Rightarrow \mathbf{a} = \mathbf{F}/m)$ and then multiplying by a unit vector $(0, 0, 1)$

$\Delta t$ is the lapse of time between t and the next tick (t+1)

## Latent variable

$$\mathbf{q}^{(i)}_{t+1} = \mathbf{q}^{(i)}_t*R2Q((\mathbf{\boldsymbol{\omega}_C}_t+\mathbf{\boldsymbol{\omega}_C}^\epsilon_t)*\Delta t)$$

Where R2Q is a function that convert from radians of the angular rotation, to a quaternion and $\mathbf{\boldsymbol{\omega}_C}^\epsilon_t$ represents the error from the control input and is sampled from $\mathbf{\boldsymbol{\omega}_C}^\epsilon_t \sim \mathcal{N}(\mathbf{0}, \mathbf{R}_{\mathbf{\boldsymbol{\omega}_C}_t })$

Initial attitude $\mathbf{q_0}$ is sampled such that the drone pitch and roll are none (parralel to the ground) but the yaw is unknown and uniformly distributed.

## Model dynamics

- $\mathbf{a}(t+1) = \mathbf{R}_{b2f}\{\mathbf{q}(t+1)\}\mathbf{T}_{2a} {t_C}(t+1) + \mathbf{a}^\epsilon_t$ where $\mathbf{a}^\epsilon_t \sim \mathcal{N}(\mathbf{0}, \mathbf{Q}_{\mathbf{a}_t })$
- $\mathbf{v}(t+1) = \mathbf{v}(t) + \Delta t \mathbf{a}(t) + \mathbf{v}^\epsilon_t$ where $\mathbf{v}^\epsilon_t \sim \mathcal{N}(\mathbf{0}, \mathbf{Q}_{\mathbf{v}_t })$
- $\mathbf{p}(t+1) = \mathbf{p}(t) + \Delta t \mathbf{v}(t) + \mathbf{p}^\epsilon_t$ where $\mathbf{p}^\epsilon_t \sim \mathcal{N}(\mathbf{0}, \mathbf{Q}_{\mathbf{p}_t })$

Note that $\mathbf{q}(t+1)$ is known because the model is conditionned under $\boldsymbol{\theta}^{(i)}_{t+1}$.

The model dynamic define the state-transition matrix $\mathbf{F}_t\{\boldsymbol{\theta}^{(i)}_t\}$, the control-input matrix $\mathbf{B}_t\{\boldsymbol{\theta}^{(i)}_t\}$, the process noise $\mathbf{w}_t\{\boldsymbol{\theta}^{(i)}_t\}$ for the Kalman filter and its covariance $\mathbf{Q}_t\{\boldsymbol{\theta}^{(i)}_t\}$

$$\mathbf{x}_t = \mathbf{F}_t\{\boldsymbol{\theta}^{(i)}_t\} \mathbf{x}_{t-1} + \mathbf{B}_t\{\boldsymbol{\theta}^{(i)}_t\} \mathbf{u}_t + \mathbf{w}_t\{\boldsymbol{\theta}^{(i)}_t\}$$


$$\mathbf{F}_t\{\boldsymbol{\theta}^{(i)}_t\}_{9 \times 9} = 
\left( \begin{array}{ccc}
\mathbf{0}_{3 \times 3} & \mathbf{0}_{3 \times 3} & \mathbf{0}_{3 \times 3} \\
\Delta t~\mathbf{I}_{3 \times 3} & \mathbf{I}_{3 \times 3} & \mathbf{0}_{3 \times 3} \\
\mathbf{0}_{3 \times 3} & \Delta t~\mathbf{I}_{3 \times 3} & \mathbf{I}_{3 \times 3}
\end{array} \right)$$

$$\mathbf{B}_t\{\boldsymbol{\theta}^{(i)}_t\}_{9 \times 4} = 
\left( \begin{array}{ccc}
\mathbf{0}_{3 \times 3} & \mathbf{R}_{b2f}\{\mathbf{q}^{(i)}_{t}\}\mathbf{T}_{2a} \\
\mathbf{0}_{3 \times 3} & \mathbf{0}_{3 \times 1} \\
\mathbf{0}_{3 \times 3} & \mathbf{0}_{3 \times 1}
\end{array} \right)$$

$$\mathbf{Q}_t\{\boldsymbol{\theta}^{(i)}_t\}_{9 \times 9} = 
\left( \begin{array}{ccc}
\mathbf{Q}_{\mathbf{a}_t } & \mathbf{0}_{3 \times 3} & \mathbf{0}_{3 \times 3} \\
\mathbf{0}_{3 \times 3} & \mathbf{Q}_{\mathbf{v}_t } & \mathbf{0}_{3 \times 3} \\
\mathbf{0}_{3 \times 3} & \mathbf{0}_{3 \times 3} & \mathbf{Q}_{\mathbf{p}_t }
\end{array} \right)$$

## Kalman prediction

$$ \mathbf{x}^{-(i)}_t = \mathbf{F}_t\{\boldsymbol{\theta}^{(i)}_t\} \mathbf{x}^{(i)}_{t-1} + \mathbf{B}_t\{\boldsymbol{\theta}^{(i)}_t\} \mathbf{u}_t $$
$$ \mathbf{\Sigma}^{-(i)}_t = \mathbf{F}_t\{\boldsymbol{\theta}^{(i)}_t\} \mathbf{\Sigma}^{-(i)}_{t-1}  (\mathbf{F}_t\{\boldsymbol{\theta}^{(i)}_t\})^T + \mathbf{w}_t\{\boldsymbol{\theta}^{(i)}_t\}$$

## Measurements model

The measurement model defines how to compute $p(\mathbf{y}_t | \boldsymbol{\theta}^{(i)}_{0:t-1}, \mathbf{y}_{1:t-_K1})$

- Accelerometer: 
    1. $\mathbf{a}(t) = \mathbf{R}_{b2f}\{\mathbf{q}(t)\}(\mathbf{a_A}(t) + \mathbf{a_A}^\epsilon_t)$ where $\mathbf{a_A}^\epsilon_t \sim \mathcal{N}(\mathbf{0}, \mathbf{R}_{\mathbf{a_A}_t })$
    2. $\mathbf{g}^f(t) = \mathbf{R}_{b2f}\{\mathbf{q}(t)\}(\mathbf{a_A}(t) + \mathbf{a_A}^\epsilon_t) - \mathbf{a}(t)$
- Gyroscope: 
    3. $\mathbf{q}(t) = \mathbf{q}(t-1) + \Delta t(\mathbf{\boldsymbol{\omega}_G}(t) + \mathbf{\boldsymbol{\omega}_G}^\epsilon_t)$ where $\mathbf{\boldsymbol{\omega}_G^\epsilon}_t \sim \mathcal{N}(\mathbf{0}, \mathbf{R}_{\mathbf{\boldsymbol{\omega}_G}_t })$
- Vicon: 
    4. $\mathbf{p}(t) = \mathbf{p_V}(t) + \mathbf{p_V}^\epsilon_t$ where $\mathbf{p_V}^\epsilon_t \sim \mathcal{N}(\mathbf{0}, \mathbf{R}_{\mathbf{p_V}_t })$
    5. $\mathbf{q}(t) = \mathbf{q_V}(t) + \mathbf{q_V}^\epsilon_t$ where $\mathbf{q_V}^\epsilon_t \sim \mathcal{N}(\mathbf{0}, \mathbf{R}_{\mathbf{q_V}_t })$	

(1, 4) define the observation matrix $\mathbf{H}_t\{\boldsymbol{\theta}^{(i)}_t\}$, the observation noise $\mathbf{v}_t\{\boldsymbol{\theta}^{(i)}_t\}$ and its covariance matrix $\mathbf{R}_t\{\boldsymbol{\theta}^{(i)}_t\}$ for the Kalman filter.

$$(\mathbf{a_A}_t, \mathbf{p_V}_t)^T  = \mathbf{H}_t\{\boldsymbol{\theta}^{(i)}_t\} (\mathbf{a}_t, \mathbf{p}_t)^T + \mathbf{v}_t\{\boldsymbol{\theta}^{(i)}_t\}$$	

$$\mathbf{H}_t\{\boldsymbol{\theta}^{(i)}_t\}_{6 \times 6} = 
\left( \begin{array}{ccc}
\mathbf{R}_{f2b}\{\mathbf{q}^{(i)}_{t}\} & \mathbf{0}_{3 \times 3} \\
\mathbf{0}_{3 \times 3} & \mathbf{I}_{3 \times 3} 
\end{array} \right)$$


$$\mathbf{R}_t\{\boldsymbol{\theta}^{(i)}_t\}_{6 \times 6} = 
\left( \begin{array}{ccc}
\mathbf{R}_{f2b}\{\mathbf{q}^{(i)}_{t}\}\mathbf{R}_{\mathbf{a_A}_t } & \mathbf{0}_{3 \times 3} \\
\mathbf{0}_{3 \times 3} & \mathbf{R}_{\mathbf{p_V}_t }
\end{array} \right)$$

### Kalman update

$$\mathbf{S} = \mathbf{H}_t\{\boldsymbol{\theta}^{(i)}_t\} \mathbf{\Sigma}^{-(i)}_t  (\mathbf{H}_t\{\boldsymbol{\theta}^{(i)}_t\})^T + \mathbf{R}_t\{\boldsymbol{\theta}^{(i)}_t\})$$
$$\mathbf{\hat{z}} = \mathbf{H}_t\{\boldsymbol{\theta}^{(i)}_t\}  \mathbf{x}^{-(i)}_t$$
$$\mathbf{K} = \mathbf{\Sigma}^{-(i)}_t \mathbf{H}_t\{\boldsymbol{\theta}^{(i)}_t\}^T \mathbf{S}^{-1}$$
$$\mathbf{\Sigma}^{(i)}_t = \mathbf{\Sigma}^{(i)}_t + \mathbf{K} \mathbf{S} \mathbf{K}^T$$
$$\mathbf{x}^{(i)}_t = \mathbf{H}_t\{\boldsymbol{\theta}^{(i)}_t\} \mathbf{x}^{(i)}_{t-1} + \mathbf{K}((\mathbf{a_A}_t, \mathbf{p_V}_t)^T - \mathbf{\hat{z}})$$
$$p(\mathbf{y}_t | \boldsymbol{\theta}^{(i)}_{0:t-1}, \mathbf{y}_{1:t-1}) = \mathcal{N}((\mathbf{a_A}_t, \mathbf{p_V}_t)^T; \mathbf{\hat{z}}_t, \mathbf{S})$$

### Asynchronous measurements

Our measurements from the Vicon and the accelerometer have different sampling rate so instead of doing full kalman update, we only apply a partial kalman update corresponding to the current type of measurement $\mathbf{z}_t$

For instance, we would apply the kalman update from the previous section but with: 

- for $\mathbf{a_A}_t$: 
$$\mathbf{H}_t\{\boldsymbol{\theta}^{(i)}_t\}_{3 \times 6} = (\mathbf{R}_{f2b}\{\mathbf{q}^{(i)}_{t}\} \mathbf{0}_{3 \times 3})$$
$$\mathbf{R}_t\{\boldsymbol{\theta}^{(i)}_t\}_{3 \times 3} = \mathbf{R}_{f2b}\{\mathbf{q}^{(i)}_{t}\}\mathbf{R}_{\mathbf{a_A}_t } $$
$$\mathbf{x}^{(i)}_t = \mathbf{H}_t\{\boldsymbol{\theta}^{(i)}_t\} \mathbf{x}^{(i)}_{t-1} + \mathbf{K}(\mathbf{a_A}_t - \mathbf{\hat{z}})$$
$$p(\mathbf{y}_t | \boldsymbol{\theta}^{(i)}_{0:t-1}, \mathbf{y}_{1:t-1}) = \mathcal{N}(\mathbf{a_A}_t; \mathbf{\hat{z}}_t, \mathbf{S})$$


- for $\mathbf{p_V}_t$: 
$$\mathbf{H}_t\{\boldsymbol{\theta}^{(i)}_t\}_{3 \times 6} = (\mathbf{0}_{3 \times 3} \mathbf{I}_{3 \times 3} )$$
$$\mathbf{R}_t\{\boldsymbol{\theta}^{(i)}_t\}_{3 \times 3} =  \mathbf{R}_{\mathbf{p_V}_t }$$
$$\mathbf{x}^{(i)}_t = \mathbf{H}_t\{\boldsymbol{\theta}^{(i)}_t\} \mathbf{x}^{(i)}_{t-1} + \mathbf{K}(\mathbf{p_V}_t - \mathbf{\hat{z}})$$
$$p(\mathbf{y}_t | \boldsymbol{\theta}^{(i)}_{0:t-1}, \mathbf{y}_{1:t-1}) = \mathcal{N}(\mathbf{p_V}_t; \mathbf{\hat{z}}_t, \mathbf{S})$$

## Other sources or reweighting

(2, 3 and 5) defines three other weight updates.


$$p(\mathbf{y}_t | \boldsymbol{\theta}^{(i)}_{0:t-1}, \mathbf{y}_{1:t-1}) = \mathcal{N}(\mathbf{a_A}_t; \mathbf{R}_{f2b}\{\mathbf{q}^{(i)}_t\}\mathbf{g} + \mathbf{a}^{(i)}_t,~ \mathbf{R}_{\mathbf{a_A}_t} + \mathbf{Pa}^{-(i)}_t)$$

$$p(\mathbf{y}_t | \boldsymbol{\theta}^{(i)}_{0:t-1}, \mathbf{y}_{1:t-1}) = \mathcal{N}(\mathbf{\boldsymbol{\omega}_G}_t; (\mathbf{q}^{(i)}_t - \mathbf{q}^{(i)}_{t-1})/\Delta t,~ \mathbf{R}_{\mathbf{\boldsymbol{\omega}_G}_t})$$

$$p(\mathbf{y}_t | \boldsymbol{\theta}^{(i)}_{0:t-1}, \mathbf{y}_{1:t-1}) = \mathcal{N}(\mathbf{q_V}_t; \mathbf{q}^{(i)}_t,~ \mathbf{R}_{\mathbf{q_V}_t })$$


**TODO**: Check that matrix of covariance is correct for 5. Found covariance as covariance of sum of normal but seems too simple.

where $\mathbf{Pa}^{-(i)}_t$ is the variance of $\mathbf{a}$ in $\mathbf{\Sigma}^{-(i)}_t$ and $\mathbf{g}$ is the gravity vector.

## Algorithm summary

1. Initiate $N$ particles with $\mathbf{p}_0$, $\mathbf{q}_0 ~ \sim p(\mathbf{q}_0)$, $\mathbf{\Sigma}_0$ and $w = 1/N$ 
2. While new sensor measurements $(\mathbf{z}_t, \mathbf{u}_t)$ 
   - foreach $N$ particles $(i)$:
       1. sample new latent variable $\boldsymbol{\theta_t}$ from $\mathbf{u}_t$
       2. Depending on the type of measurement:
     	   - **Accelerometer**:
		   Partial kalman update with: 
		   $$\mathbf{H}_t\{\boldsymbol{\theta}^{(i)}_t\}_{3 \times 6} = (\mathbf{R}_{f2b}\{\mathbf{q}^{(i)}_{t}\} \mathbf{0}_{3 \times 3})$$
		   $$\mathbf{R}_t\{\boldsymbol{\theta}^{(i)}_t\}_{3 \times 3} = \mathbf{R}_{f2b}\{\mathbf{q}^{(i)}_{t}\}\mathbf{R}_{\mathbf{a_A}_t } $$
		   $$\mathbf{x}^{(i)}_t = \mathbf{H}_t\{\boldsymbol{\theta}^{(i)}_t\} \mathbf{x}^{(i)}_{t-1} + \mathbf{K}(\mathbf{a_A}_t - \mathbf{\hat{z}})$$
		   $$p(\mathbf{y}_t | \boldsymbol{\theta}^{(i)}_{0:t-1}, \mathbf{y}_{1:t-1})^- = \mathcal{N}(\mathbf{a_A}_t; \mathbf{\hat{z}}_t, \mathbf{S})$$
		   $$p(\mathbf{y}_t | \boldsymbol{\theta}^{(i)}_{0:t-1}, \mathbf{y}_{1:t-1}) = \mathcal{N}(\mathbf{a_A}_t; \mathbf{R}_{f2b}\{\mathbf{q}^{(i)}_t\}\mathbf{g} + \mathbf{a}^{(i)}_t, \mathbf{R}_{\mathbf{a_A}_t} + \mathbf{Pa}^{-(i)}_t) p(\mathbf{y}_t | \boldsymbol{\theta}^{(i)}_{0:t-1}, \mathbf{y}_{1:t-1})^-$$
           - **Gyroscope**: 
		   $$p(\mathbf{y}_t | \boldsymbol{\theta}^{(i)}_{0:t-1}, \mathbf{y}_{1:t-1}) = \mathcal{N}(\mathbf{\boldsymbol{\omega}_G}_t; (\mathbf{q}^{(i)}_t - \mathbf{q}^{(i)}_{t-1})/\Delta t,~ \mathbf{R}_{\mathbf{\boldsymbol{\omega}_G}_t})$$
     	   - **Vicon**: 
		   Partial kalman update with: 
		   $$\mathbf{H}_t\{\boldsymbol{\theta}^{(i)}_t\}_{3 \times 6} = (\mathbf{0}_{3 \times 3} \mathbf{I}_{3 \times 3} )$$
		   $$\mathbf{R}_t\{\boldsymbol{\theta}^{(i)}_t\}_{3 \times 3} =  \mathbf{R}_{\mathbf{p_V}_t }$$
		   $$\mathbf{x}^{(i)}_t = \mathbf{H}_t\{\boldsymbol{\theta}^{(i)}_t\} \mathbf{x}^{(i)}_{t-1} + \mathbf{K}(\mathbf{p_V}_t - \mathbf{\hat{z}})$$
		   $$p(\mathbf{y}_t | \boldsymbol{\theta}^{(i)}_{0:t-1}, \mathbf{y}_{1:t-1})^- = \mathcal{N}(\mathbf{p_V}_t; \mathbf{\hat{z}}_t, \mathbf{S})$$
		   $$p(\mathbf{y}_t | \boldsymbol{\theta}^{(i)}_{0:t-1}, \mathbf{y}_{1:t-1}) = \mathcal{N}(\mathbf{q_V}_t; \mathbf{q}^{(i)}_t,~ \mathbf{R}_{\mathbf{q_V}_t }) p(\mathbf{y}_t | \boldsymbol{\theta}^{(i)}_{0:t-1}, \mathbf{y}_{1:t-1})^-$$
		   
      3. Update $w^{(i)}_t$: $w^{(i)}_t = p(\mathbf{y}_t | \boldsymbol{\theta}^{(i)}_{0:t-1}, \mathbf{y}_{1:t-1}) w^{(i)}_{t-1}$	  
  - Normalize all $w^{(i)}$ by scalaing by $1/(\sum w^{(i)})$ such that $\sum w^{(i)}= 1$
  - Compute $\mathbf{p}_t$ and $\mathbf{q}_t$ as the expectation from the distribution approximated by the N particles.
  - Resample if the number of effective particle is too low


