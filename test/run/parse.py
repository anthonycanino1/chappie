#!/usr/bin/python3

import os
import pandas as pd

# chappie_times = [len(pd.read_csv(file)) for file in os.listdir('.') if 'trace' in file]

df = pd.concat([pd.read_csv(file) for file in os.listdir('.') if 'trace' in file and ('data' not in file and 'stats' not in file)], ignore_index = True)
df.to_csv('chappie.trace.data.csv')

agg_df = df.groupby(['state'])['count'].agg(['mean', 'std'])
agg_df.to_csv('chappie.trace.stats.csv')

chappie_time = df['time'].max()

df = pd.concat([pd.read_csv(file) for file in os.listdir('.') if 'thread' in file and ('data' not in file and 'stats' not in file)], ignore_index = True)
df['core'] = df['core'].fillna(-1)
df['core'] = (df['core'] != df['core'].shift(1)).map(lambda x: 1 if x else 0)
agg_df = df.groupby(['time', 'thread']).mean()
agg_df.to_csv('chappie.thread.data.csv')

agg_df = df.groupby(['thread']).agg({'time': 'max', 'core': 'std', 'package': ['sum', 'std'], \
                                          'dram': ['sum', 'std'], \
                                          'bytes': ['max', 'std']})
agg_df.columns = ['_'.join(col).strip() for col in agg_df.columns.values]
agg_df.to_csv('chappie.thread.stats.csv')

# clock_times = [int(open(file, 'r').read()) for file in os.listdir('.') if 'chappiebench.time' in file]
#
# df = pd.DataFrame({'chappie_time': chappie_times, 'clock_time': clock_times})
# df['overhead'] = df['clock_time'] - df['chappie_time']
# df.to_csv('chappiebench.time.data.csv')
#
# agg_df = df.agg(['max', 'std'])
# agg_df.to_csv('chappiebench.time.stats.csv')
