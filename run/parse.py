#!/usr/bin/python3

import os
import pandas as pd

df = pd.concat([pd.read_csv(file) for file in os.listdir('.') if 'trace' in file and ('data' not in file and 'stats' not in file)], ignore_index = True)
df.to_csv('chappie.trace.data.csv')

from tabulate import tabulate
print()
print('Thread Activity Summary')
agg_df = df.groupby(['state'])['count'].agg(['mean', 'std']).rename(columns = {'mean': 'Average', 'std': 'Deviation'})
agg_df.index.name = 'State'
table = tabulate(agg_df, headers = 'keys', tablefmt = 'psql')
with open('chappie.trace.stats.csv', 'w+') as f:
    f.write(table)
print(table)

chappie_time = df['time'].max()

df = pd.concat([pd.read_csv(file) for file in os.listdir('.') if 'thread' in file and ('data' not in file and 'stats' not in file)], ignore_index = True)
df['core'] = df['core'].fillna(-1)
df['core'] = (df['core'] != df['core'].shift(1)).map(lambda x: 1 if x else 0)

agg_df = df.groupby(['time', 'thread']).agg({'core': 'sum',     \
                                            'package': 'mean',  \
                                            'dram': 'mean',     \
                                            'bytes': 'mean'})
agg_df[['core', 'package', 'dram', 'bytes']].to_csv('chappie.thread.data.csv')

agg_df = df.groupby(['thread']).agg({'time': 'min',         \
                                        'core': 'sum',      \
                                        'package': 'sum',   \
                                        'dram': 'sum',      \
                                        'bytes': 'sum'})


agg_df['time_2'] = df.fillna(-1).groupby(['thread'])['time'].max()

total = agg_df.agg({'time': 'min', 'time_2': 'max', 'core': 'sum', 'package': 'sum', 'dram': 'sum', 'bytes': 'sum'})
total.name = 'Total'
agg_df = agg_df.append(total)
agg_df = agg_df[['time', 'time_2', 'core', 'package', 'dram', 'bytes']].rename(columns = {'time': 'Start Time (ms)',        \
                                                                                            'time_2': 'End Time (ms)',      \
                                                                                            'package': 'Total Package (J)', \
                                                                                            'dram': 'Total DRAM (J)',       \
                                                                                            'bytes': 'Total Bytes',         \
                                                                                            'core': 'Core Hops'})
agg_df.agg({}).sum()
agg_df.index.name = 'Thread'
print()
table = tabulate(agg_df, headers = 'keys', tablefmt = 'psql')
with open('chappie.thread.stats.csv', 'w+') as f:
    f.write(table)
print(table)
