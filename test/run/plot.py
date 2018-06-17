#!/usr/bin/python3

import pandas as pd
import matplotlib.pyplot as plt

df = pd.read_csv('chappie.trace.data.csv').dropna(how = 'all')

time = df['time'].unique()

active = df[df['state'] == 'active']['count']
inactive = df[df['state'] == 'inactive']['count']

plt.bar(time, active, color='#d62728')
plt.bar(time, inactive, bottom = active)

plt.title('Thread Activity')
plt.xlabel('Chappie Time (ticks)')
plt.ylabel('Count')

plt.figure()
plt.xlabel('Chappie Time (ticks)')

df = pd.read_csv('chappie.thread.data.csv').set_index('time')

from collections import OrderedDict

measures = ['package', 'dram', 'bytes', 'core']
# measures = {'package': 'Energy (J)', 'dram': 'Energy (J)', 'bytes': 'Bytes', 'core': 'Switched'}
# measures = OrderedDict(sorted(measures.items(), key = lambda x: x[0]))
i = 1
for measure in measures:
   plt.subplot('22{}'.format(i))
   i += 1
   df.groupby('thread')[measure].plot()
   # plt.title(measure.title())
   plt.ticklabel_format(style='sci', axis='y', scilimits=(0,0))
   plt.ylabel(measure) # measures[measure])

plt.show()
