"""
Deep Deterministic Policy Gradient (DDPG), Reinforcement Learning.
DDPG is Actor Critic based algorithm.
Pendulum example.
View more on my tutorial page: https://morvanzhou.github.io/tutorials/
Using:
tensorflow 1.0
gym 0.8.0
"""

import tensorflow as tf
import numpy as np
import gym
import time
import requests
import pickle
import os

#####################  hyper parameters  ####################

MAX_EPISODES = 5
MAX_EP_STEPS = 2
LR_A = 0.01    # learning rate for actor
LR_C = 0.02    # learning rate for critic
GAMMA = 0.9     # reward discount
TAU = 0.01      # soft replacement
MEMORY_CAPACITY = 10000
BATCH_SIZE = 32

RENDER = False
ENV_NAME = 'Pendulum-v0'

###############################  DDPG  ####################################

class DDPG(object):
    def __init__(self, a_dim, s_dim, a_middle, a_half):
        self.memory = np.zeros((MEMORY_CAPACITY, s_dim * 2 + a_dim + 1), dtype=np.float32)
        self.pointer = 0
        self.sess = tf.Session()

        self.a_dim, self.s_dim, self.a_middle, self.a_half = a_dim, s_dim, a_middle, a_half
        self.S = tf.placeholder(tf.float32, [None, s_dim], 's')
        self.S_ = tf.placeholder(tf.float32, [None, s_dim], 's_')
        self.R = tf.placeholder(tf.float32, [None, 1], 'r')

        with tf.variable_scope('Actor'):
            self.a = self._build_a(self.S, scope='eval', trainable=True)
            a_ = self._build_a(self.S_, scope='target', trainable=False)
        with tf.variable_scope('Critic'):
            # assign self.a = a in memory when calculating q for td_error,
            # otherwise the self.a is from Actor when updating Actor
            q = self._build_c(self.S, self.a, scope='eval', trainable=True)
            q_ = self._build_c(self.S_, a_, scope='target', trainable=False)

        # networks parameters
        self.ae_params = tf.get_collection(tf.GraphKeys.GLOBAL_VARIABLES, scope='Actor/eval')
        self.at_params = tf.get_collection(tf.GraphKeys.GLOBAL_VARIABLES, scope='Actor/target')
        self.ce_params = tf.get_collection(tf.GraphKeys.GLOBAL_VARIABLES, scope='Critic/eval')
        self.ct_params = tf.get_collection(tf.GraphKeys.GLOBAL_VARIABLES, scope='Critic/target')

        # target net replacement
        self.soft_replace = [tf.assign(t, (1 - TAU) * t + TAU * e)
                             for t, e in zip(self.at_params + self.ct_params, self.ae_params + self.ce_params)]

        q_target = self.R + GAMMA * q_
        # in the feed_dic for the td_error, the self.a should change to actions in memory
        td_error = tf.losses.mean_squared_error(labels=q_target, predictions=q)
        self.ctrain = tf.train.AdamOptimizer(LR_C).minimize(td_error, var_list=self.ce_params)

        a_loss = - tf.reduce_mean(q)    # maximize the q
        self.atrain = tf.train.AdamOptimizer(LR_A).minimize(a_loss, var_list=self.ae_params)

        self.sess.run(tf.global_variables_initializer())

    def choose_action(self, s):
        return self.sess.run(self.a, {self.S: s[np.newaxis, :]})[0]

    def learn(self):
        # soft target replacement
        self.sess.run(self.soft_replace)

        indices = np.random.choice(MEMORY_CAPACITY, size=BATCH_SIZE)
        bt = self.memory[indices, :]
        bs = bt[:, :self.s_dim]
        ba = bt[:, self.s_dim: self.s_dim + self.a_dim]
        br = bt[:, -self.s_dim - 1: -self.s_dim]
        bs_ = bt[:, -self.s_dim:]

        self.sess.run(self.atrain, {self.S: bs})
        self.sess.run(self.ctrain, {self.S: bs, self.a: ba, self.R: br, self.S_: bs_})

    def store_transition(self, s, a, r, s_):
        transition = np.hstack((s, a, [r], s_))
        index = self.pointer % MEMORY_CAPACITY  # replace the old memory with new memory
        self.memory[index, :] = transition
        self.pointer += 1

    def _build_a(self, s, scope, trainable):
        with tf.variable_scope(scope):
            net = tf.layers.dense(s, 30, activation=tf.nn.relu, name='l1', trainable=trainable)
            a = tf.layers.dense(net, self.a_dim, activation=tf.nn.tanh, name='a', trainable=trainable)
            #这个为什么要scale?
            return a_middle + tf.multiply(a, self.a_half, name='scaled_a')

    def _build_c(self, s, a, scope, trainable):
        with tf.variable_scope(scope):
            n_l1 = 30
            w1_s = tf.get_variable('w1_s', [self.s_dim, n_l1], trainable=trainable)
            w1_a = tf.get_variable('w1_a', [self.a_dim, n_l1], trainable=trainable)
            b1 = tf.get_variable('b1', [1, n_l1], trainable=trainable)
            net = tf.nn.relu(tf.matmul(s, w1_s) + tf.matmul(a, w1_a) + b1) #?
            return tf.layers.dense(net, 1, trainable=trainable)  # Q(s,a)

###############################  training  ####################################

# env = gym.make(ENV_NAME)
# env = env.unwrapped
# env.seed(1)

# s_dim = env.observation_space.shape[0]
# a_dim = env.action_space.shape[0]
# a_bound = env.action_space.high
# ap = env.action_space 

# ddpg = DDPG(a_dim, s_dim, a_bound)

# var = 3  # control exploration
# t1 = time.time()
# for i in range(MAX_EPISODES):
#     s = env.reset()
#     ep_reward = 0
#     for j in range(MAX_EP_STEPS):
#         if RENDER:
#             env.render()

#         # Add exploration noise
#         a = ddpg.choose_action(s)
#         a = np.clip(np.random.normal(a, var), -2, 2)    # add randomness to action selection for exploration
#         s_, r, done, info = env.step(a)  # 这一步交互
#         #改config 跑一次 return s_
#         ddpg.store_transition(s, a, r / 10, s_)

#         if ddpg.pointer > MEMORY_CAPACITY:
#             var *= .9995    # decay the action randomness
#             ddpg.learn()

#         s = s_
#         ep_reward += r
#         if j == MAX_EP_STEPS-1:
#             print('Episode:', i, ' Reward: %i' % int(ep_reward), 'Explore: %.2f' % var, )
#             if ep_reward > -300:RENDER = True
#             break
# print('Running time: ', time.time() - t1)

#############################  self training  ##################################

data = ''
for line in open("/Users/minxingyang/Desktop/config.txt","r"): #设置文件对象并读取每一行文件
    data = data + line

dim = data.split("action and range\n")[0]
a_dim = int(dim.split("\n")[0].split(" ")[1])
s_dim = int(dim.split("\n")[1].split(" ")[1])

action_and_range = data.split("action and range\n")[1].split("initial state\n")[0]
a_r_string = action_and_range.split("\n")
a = []
a_middle = []
a_half = []
for i in range(len(a_r_string) - 1):
    a.append(float(a_r_string[i].split(" ")[1]))
    a_middle.append((float(a_r_string[i].split(" ")[3]) + float(a_r_string[i].split(" ")[2])) / 2)
    a_half.append((float(a_r_string[i].split(" ")[3]) - float(a_r_string[i].split(" ")[2])) / 2)
    
a = np.array(a)
a_middle = np.array(a_middle)
a_half = np.array(a_half)

initial_state = data.split("action and range\n")[1].split("initial state\n")[1].split("reward")[0]
s_string = initial_state.split("\n")
s = []
for i in range(len(s_string) - 1):
    s.append(float(s_string[i].split(" ")[1]))
s = np.array(s)
s_init = s
reward = data.split("action and range\n")[1].split("initial state\n")[1].split("reward")[1]
# r = float(reward.split(" ")[1])

######################################################

ddpg = DDPG(a_dim, s_dim, a_middle, a_half)
saver = tf.train.Saver()
if (os.path.exists("./ddpg/model.ckpt.meta")):
    saver.restore(ddpg.sess, "./ddpg/model.ckpt") # 注意此处路径前添加"./" 
    print("restored")

var = a_half  # control exploration
t1 = time.time()
for i in range(MAX_EPISODES):
    s = s_init
    ep_reward = 0
    for j in range(MAX_EP_STEPS):
        # Add exploration noise
        a = ddpg.choose_action(s)
        a_ran = np.random.normal(a, var)
        #这一步 要每一个的randomness
        for k in range(len(a)):    # add randomness to action selection for exploration
            if a_ran[k] > a_middle[k] + a_half[k]:
                a_ran[k] = int(a_middle[k] + a_half[k])
            elif a_ran[k] < a_middle[k] - a_half[k]:
                a_ran[k] = int(a_middle[k] - a_half[k])
            else:
                a_ran[k] = int(a_ran[k])
        a = a_ran
        params = {'action': a}
        headers = {'sender': 'python'}
        r0 = requests.get("http://localhost:8888", headers=headers, params=params)
        print(r0.text)
        s_ = []
        for k in range(len(s_string) - 1):
            s_.append(float(r0.text.split(" ")[k]))
        s_ = np.array(s_)
        r = float(r0.text.split(" ")[len(r0.text.split(" ")) - 1])
        
        # s_, r, done, info = env.step(a)  # 这一步交互
        #改config 跑一次 return s_
        ddpg.store_transition(s, a, r / 10, s_)

        if ddpg.pointer > MEMORY_CAPACITY:
            var *= .9995    # decay the action randomness
            ddpg.learn()

        s = s_
        ep_reward += r
        if j == MAX_EP_STEPS-1:
            print('Episode:', i, ' Reward: ', ep_reward)
            # 'Explore: %.2f' % var, 
            if ep_reward > -300:RENDER = True
            break
    saver.save(ddpg.sess, "./ddpg/model.ckpt")
print('Running time: ', time.time() - t1)