#!/usr/bin/python3

import os
import pandas as pd
from tabulate import tabulate

df = pd.concat([pd.read_csv(file) for file in os.listdir('.') if 'trace' in file and ('data' not in file and 'stats' not in file)], ignore_index = True)
df.to_csv('chappie.trace.data.csv')

print()
print('Thread Activity Summary')
df = df.groupby(['state'])['count'].agg(['mean', 'std']).rename(columns = {'mean': 'Average', 'std': 'Deviation'})
df.index = df.index.str.title()
df.index.name = 'State'

table = tabulate(df, headers = 'keys', tablefmt = 'psql')
print(table)
with open('chappie.trace.stats.csv', 'w+') as f:
    f.write(table + '\n')

df = pd.concat([pd.read_csv(file) for file in os.listdir('.') if 'thread' in file and ('data' not in file and 'stats' not in file)], ignore_index = True)
df['socket'] = df['core'].fillna(-1).map(lambda x: -1 if x < 0 else 0 if x < 20 else 1)
df.to_csv('chappie.thread.data.csv')

df = df.groupby('thread').agg({'time': 'mean', 'socket': 'mean', 'package': 'sum', 'dram': 'sum', 'bytes': 'sum'})
df = df[~df.index.isin(['Signal Dispatcher', 'main', 'Finalizer', 'Reference Handler'])]
df['socket'] = round(df['socket'])

socket = df.groupby('socket').agg({'time': 'mean', 'package': 'mean', 'dram': 'mean', 'bytes': 'mean'})
socket.index = ['Unmapped', 'Socket 1', 'Socket 2'][3 - len(socket.index):]
# socket.loc[socket.index == 'Socket 2', ['package', 'dram', 'bytes']] /= 10

total = df.agg({'time': 'mean', 'package': 'mean', 'dram': 'mean', 'bytes': 'mean'})
total.name = 'Total'

df = socket.append(total)

df = df[['time', 'package', 'dram', 'bytes']].rename(columns = {'time': 'Average Duration (ms)', 'package': 'Average Package (J)', 'dram': 'Average DRAM (J)', 'bytes': 'Average Bytes'})
df.index.name = 'Socket'

print()
table = tabulate(df, headers = 'keys', tablefmt = 'psql')
print(table)
with open('chappie.thread.stats.csv', 'w+') as f:
    f.write(table)
