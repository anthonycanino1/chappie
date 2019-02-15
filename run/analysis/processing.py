#!/usr/bin/python3

import argparse
import os

from csv import writer
from io import StringIO
from time import time

import numpy as np
import pandas as pd

rapl_wrap_around = 16384

filter_cpu = lambda s: [int(w.replace('cpu', '')) for w in s]

def df_diff(df, by, values):
    return pd.concat([
        df.rename(columns = {value: value + '_' for value in values}),
        df.groupby(by)[values].diff().fillna(0)
    ], axis = 1).drop(columns = [value + '_' for value in values])

if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('-path', default = "chappie_test")
    parser.add_argument('-destination', default = None)
    args = parser.parse_args()

    # setup the paths
    if not os.path.exists(args.path):
        raise FileNotFoundError('No directory at {}'.format(os.path.abspath(args.path)))

    if args.destination is None:
        args.destination = os.path.join(args.path, 'processed')

    if not os.path.exists(args.destination):
        os.mkdir(args.destination)

    runtime = np.sort([os.path.join(args.path, f) for f in os.listdir(args.path) if 'runtime' in f])

    threads = np.sort([os.path.join(args.path, f) for f in os.listdir(args.path) if 'thread' in f])
    energies = np.sort([os.path.join(args.path, f) for f in os.listdir(args.path) if 'energy' in f])
    jiffies = np.sort([os.path.join(args.path, f) for f in os.listdir(args.path) if 'system' in f])
    stacks = np.sort([os.path.join(args.path, f) for f in os.listdir(args.path) if 'stack' in f])

    activeness = np.sort([os.path.join(args.path, f) for f in os.listdir(args.path) if 'activeness' in f])

    for k, names in enumerate(zip(runtime, threads, energies, jiffies, stacks, activeness)):
        runtime, thread, energy, jiffy, stack, activity = [pd.read_csv(f) if 'stack' not in f else pd.read_csv(f, header = None) for f in names]

        # grab the runtime values
        application_runtime = runtime[runtime['name'] == 'runtime']['value']
        pid = runtime[runtime['name'] == 'main_id']['value'].max()

        ########## ENERGY PROCESSING ##########
        # compute the differential energy
        start = time()
        energy = pd.merge(energy, energy.groupby('socket').min(), on = 'socket', suffixes = ('', '_'))
        energy['package'] -= energy['package_']
        energy['dram'] -= energy['dram_']
        energy = energy[['epoch', 'socket', 'package', 'dram']]

        energy = df_diff(energy, 'socket', ['package', 'dram'])

        energy['package'] = energy['package'].map(lambda x: max(x + rapl_wrap_around, 0) if x < 0 else x)
        energy['dram'] = energy['dram'].map(lambda x: max(x + rapl_wrap_around, 0) if x < 0 else x)

        energy.to_csv(os.path.join(args.destination, 'chappie.energy.{}.csv'.format(k)), index = False)
        print('{:.2f} seconds for energy'.format(time() - start))

        ########## JIFFIES PROCESSING ##########
        # separate each line into the desired pieces
        start = time()
        jiffy = jiffy['record'][jiffy['record'].str.contains(r'cpu\d+')]

        jiffy = jiffy.str.replace('  ', ' ')
        jiffy = jiffy.str.split(' ')
        jiffy = jiffy.map(filter_cpu).values.tolist()
        jiffy = pd.DataFrame(jiffy, columns = ['core'] + ['jiffies{}'.format(i) for i in range(10)], dtype = int)
        jiffy['epoch'] = [n for N in [[i] * 40 for i in range(len(jiffy)//40)] for n in N]
        jiffy['socket'] = jiffy['core'].map(lambda x: 1 if x < 20 else 2)

        jiffy = jiffy.groupby(['epoch', 'socket']).sum().drop(columns = 'jiffies3')
        jiffy['jiffies'] = sum(jiffy[col] for col in jiffy.columns if 'jiffies' in col)
        jiffy = jiffy.drop(columns = [col for col in jiffy.columns if 'jiffies' in col and col != 'jiffies']).reset_index()

        jiffy = pd.merge(jiffy, jiffy.groupby('socket').min(), on = 'socket', suffixes = ('', '_')).reset_index()
        jiffy['jiffies'] -= jiffy['jiffies_']
        jiffy = df_diff(jiffy, 'socket', ['jiffies'])

        jiffy = jiffy[['epoch', 'socket', 'jiffies']]

        print('{:.2f} seconds for jiffies'.format(time() - start))

        ########## TID/STAT PROCESSING ##########
        # extract the name (1), state (2), jiffies (13, 14), and core (38)
        start = time()
        thread = thread.dropna(subset = ['record']).reset_index(drop = True)
        def parse_stats_records(records):
            name_size = len(records) - 52
            name = ' '.join(records[1:(name_size + 2)])[1:-1]
            state = 1 if records[2 + name_size] == 'R' else 0
            u_jiffies = int(records[13 + name_size])
            k_jiffies = int(records[14 + name_size])
            core = 1 if int(records[38 + name_size]) < 20 else 2

            return [name, state, u_jiffies, k_jiffies, core]

        thread_records = thread['record'].str.split(' ').map(parse_stats_records)

        with StringIO() as record_data:
            writer(record_data).writerows(list(thread_records.values))
            record_data.seek(0)
            thread_records = pd.read_csv(record_data, header = None)

        thread_records.columns = ['thread', 'state', 'u_jiffies', 'k_jiffies', 'socket']
        thread = pd.concat([thread, thread_records], axis = 1)
        thread = thread[['epoch', 'timestamp', 'tid', 'thread', 'state', 'u_jiffies', 'k_jiffies', 'socket']]
        thread.loc[thread['tid'] == pid, 'thread'] = 'main'

        # rescale jiffies to min
        thread = pd.merge(thread, thread.groupby('tid').min(), on = 'tid', suffixes = ('', '_'))
        thread['u_jiffies'] = thread['u_jiffies'] - thread['u_jiffies_']
        thread['k_jiffies'] = thread['k_jiffies'] - thread['k_jiffies_']

        # get differential jiffies
        thread = df_diff(thread, 'tid', ['u_jiffies', 'k_jiffies'])

        # normalize the state
        thread.loc[thread['thread'] == 'Chaperone', 'state'] = activity['activeness'].values
        thread = pd.merge(thread, thread.groupby(['epoch', 'socket'])['state'].sum().reset_index(), on = ['epoch', 'socket'], suffixes = ('', '_sum'))
        thread['state'] = (thread['state'] / thread['state_sum']).fillna(0)

        # get the ratio of application and os jiffies
        thread['jiffies'] = thread['u_jiffies'] + thread['k_jiffies']

        os_state = pd.merge(jiffy, thread.groupby(['epoch', 'socket'])['jiffies'].sum().reset_index(), on = ['epoch', 'socket'], suffixes = ('_', ''))
        os_state['os_state'] = (os_state['jiffies'] / os_state['jiffies_']).fillna(0).replace(np.inf, 0)
        os_state.loc[os_state['os_state'] > 1, 'os_state'] = 1
        os_state = os_state[['epoch', 'socket', 'os_state']]

        thread = pd.merge(thread, os_state, on = ['epoch', 'socket'])
        thread['state'] *= thread['os_state']
        print('{:.2f} seconds for thread'.format(time() - start))

        # compute the energy attribution
        thread = pd.merge(thread, energy, on = ['epoch', 'socket'])
        thread['package'] *= thread['state']
        thread['package'] = thread['package']
        thread['dram'] *= thread['state']
        thread['dram'] = thread['dram']
        thread = thread[['epoch', 'timestamp', 'thread', 'socket', 'package', 'dram']]

        thread = thread[['epoch', 'thread', 'timestamp', 'socket', 'package', 'dram']]

        # TODO:
        # revise this step after going over it with Anthony

        # stitch the methods in
        start = time()

        stack.columns = ['thread', 'timestamp', '?', 'stack']
        thread = pd.merge(thread, stack, on = 'thread')

        thread['timestamp_diff'] = thread['timestamp_y'] - thread['timestamp_x']
        thread.loc[thread['timestamp_diff'] < 0, 'timestamp_diff'] = np.iinfo(np.int64).max

        thread = pd.merge(thread, thread.groupby(['epoch', 'thread'])['timestamp_diff'].min(), on = ['epoch', 'thread'])
        thread = thread[thread['timestamp_diff_x'] == thread['timestamp_diff_y']]

        thread['stack'] = thread['stack'].fillna('end')
        thread = thread[['epoch', 'thread', 'socket', 'package', 'dram', 'stack']]

        # thread filtering goes here
        thread['all'] = thread['stack'].str.split(';').map(lambda x: 'end' if 'java' in x[0] else ';'.join(x))

        # thread['all'] = thread['stack'].map(lambda x: 'end' if 'java' in x[0] else ';'.join(x))
        # thread['all_top'] = thread['stack'].map(lambda x: 'end' if 'java' in x[0] else x[0])

        thread['unfiltered'] = thread['stack']

        def filter_to_application(l):
            if l == 'end':
                return l
            else:
                while len(l) > 1:
                    if 'java' in l[0]:
                        l.pop()
                    else:
                        break

                return ';'.join(l)

        thread['deep'] = thread['stack'].str.split(';').map(filter_to_application)

        thread = thread.drop(columns = ['stack']).sort_values('epoch')

        print('{:.2f} seconds for methods'.format(time() - start))

        print(thread.head())
        thread.to_csv(os.path.join(args.destination, 'chappie.thread.{}.csv'.format(k)), index = False)
