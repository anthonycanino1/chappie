#!/usr/bin/python3

import argparse
import os
import subprocess

from csv import writer
from io import BytesIO, StringIO
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
    ids = np.sort([os.path.join(args.path, f) for f in os.listdir(args.path) if 'id' in f])
    energies = np.sort([os.path.join(args.path, f) for f in os.listdir(args.path) if 'energy' in f])
    jiffies = np.sort([os.path.join(args.path, f) for f in os.listdir(args.path) if 'system' in f])
    stacks = np.sort([os.path.join(args.path, f) for f in os.listdir(args.path) if 'stack' in f])

    activeness = np.sort([os.path.join(args.path, f) for f in os.listdir(args.path) if 'activeness' in f])

    for k, names in enumerate(zip(runtime, threads, ids, energies, jiffies, stacks, activeness)):
        runtime, thread, id, energy, jiffy, stack, activity = [pd.read_csv(f) if 'stack' not in f else pd.read_csv(f, header = None) for f in names]

        # grab the runtime values
        application_runtime = runtime[runtime['name'] == 'runtime']['value']
        pid = runtime[runtime['name'] == 'main_id']['value'].max()

        application_runtime.to_csv(os.path.join(args.destination, 'chappie.runtime.csv'), mode = 'a', index = False, header = False)

        ########## ENERGY PROCESSING ##########
        # compute the differential energy
        start = time()

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

        jiffy = df_diff(jiffy, 'socket', ['jiffies'])[['epoch', 'socket', 'jiffies']]

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
        thread = thread[['epoch', 'timestamp', 'thread', 'tid', 'socket', 'package', 'dram']]

        id = id[['thread', 'id']].drop_duplicates()
        thread = pd.merge(thread, id, on = 'thread', how = 'left')[['epoch', 'timestamp', 'thread', 'tid', 'id', 'socket', 'package', 'dram']]
        thread['id'] = thread['id'].fillna(-1).astype(int)

        start = time()

        stack.columns = ['thread', 'timestamp', 'id', 'stack']
        df = pd.merge(thread, stack.reset_index(), on = 'thread', how = 'right', suffixes = ('', '_'))
        df['diff'] = abs(df['timestamp'] - df['timestamp_'])
        df = df.sort_values(['diff', 'index']).drop_duplicates(['epoch', 'thread']).drop_duplicates('index').sort_values('epoch')
        df = df[['epoch', 'thread', 'stack']]

        thread = pd.merge(thread, df, on = ['epoch', 'thread'], how = 'left')
        thread['stack'] = thread['stack'].fillna('end').astype(str)

        ## this is still having issues
        # thread_path = os.path.join(args.destination, 'chappie.thread.{}.csv'.format(k))
        # thread.to_csv(thread_path, index = False)
        #
        # method_path = names[4]
        #
        # ruby_args = ['-c', thread_path , '-h' , method_path]
        #
        # thread = subprocess.check_output(['./analysis/opt-align.rb'] + ruby_args)
        # thread = pd.read_csv(BytesIO(thread))

        thread['all'] = thread['stack'].str.split(';').map(lambda x: 'end' if 'java' in x[0] else ';'.join(x))
        thread['unfiltered'] = thread['stack']
        thread['deep'] = thread['stack'].str.split(';').map(filter_to_application)

        thread = thread.drop(columns = ['stack']).sort_values('epoch')

        print('{:.2f} seconds for methods'.format(time() - start))

        thread_path = os.path.join(args.destination, 'chappie.thread.{}.csv'.format(k))
        thread.to_csv(thread_path, index = False)
