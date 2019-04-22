#!/usr/bin/python3


import argparse
import os
import subprocess

from csv import writer
from io import BytesIO, StringIO
from time import time

import numpy as np
import pandas as pd
import scipy.stats as sps

rapl_wrap_around = 16384

filter_cpu = lambda s: [int(w.replace('cpu', '')) for w in s]

def df_diff(df, by, values):
    df = pd.concat([
        df.rename(columns = {value: value + '_' for value in values}),
        df.groupby(by)[values].diff().fillna(0)
    ], axis = 1)
    df.drop(columns = [value + '_' for value in values])

    return df

def filter_to_application(l):
    if l == 'end':
        return l
    else:
        while len(l) > 1:
            if 'java' in l[0]:
                l.pop(0)
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
    if os.path.exists(os.path.join(args.path, 'chappie.stack.csv')):
        stacks = len(jiffies) * [os.path.join(args.path, 'chappie.stack.csv')]
        # np.sort([os.path.join(args.path, f) for f in os.listdir(args.path) if 'stack' in f])
    else:
        stacks = np.sort([os.path.join(args.path, f) for f in os.listdir(args.path) if 'stack' in f])

    activeness = np.sort([os.path.join(args.path, f) for f in os.listdir(args.path) if 'activeness' in f])

    # runtime_summary = []

    # from tqdm import tqdm

    for k, names in enumerate(zip(runtime, threads, ids, energies, jiffies, stacks, activeness)):
        # if k < len(runtime) / 4:
        #     continue

        runtime, id, energy, jiffy, stack, activity = [pd.read_csv(f) if 'stack' not in f else pd.read_csv(f, header = None) for f in names if 'thread' not in f]

        # grab the runtime values
        application_runtime = runtime[runtime['name'] == 'runtime']['value']
        pid = runtime[runtime['name'] == 'main_id']['value'].max()

        # runtime_summary.append(runtime.pivot(columns = 'name', values = 'value'))
        # print(runtime_summary[0])
        # sys.exit(0)

        application_runtime.to_csv(os.path.join(args.destination, 'chappie.runtime.csv'), mode = 'a', index = False, header = False)

        ########## ENERGY PROCESSING ##########
        # compute the differential energy
        start = time()

        energy['package'] = energy.groupby('socket')['package'].apply(pd.Series.diff).fillna(0)
        energy['dram'] = energy.groupby('socket')['dram'].apply(pd.Series.diff).fillna(0)

        energy['package'] = energy['package'].map(lambda x: max(x + rapl_wrap_around, 0) if x < 0 else x)
        energy['dram'] = energy['dram'].map(lambda x: max(x + rapl_wrap_around, 0) if x < 0 else x)

        energy.to_csv(os.path.join(args.destination, 'chappie.energy.{}.csv'.format(k)), index = False)

        print('{:.2f} seconds for energy'.format(time() - start))

        ########## TID/STAT PROCESSING ##########
        # extract the name (1), state (2), jiffies (13, 14), and core (38)
        def parse_stats_records(records):
            name_size = len(records) - 52
            name = ' '.join(records[1:(name_size + 2)])[1:-1]
            state = 1 if records[2 + name_size] == 'R' else 0
            u_jiffies = int(records[13 + name_size])
            k_jiffies = int(records[14 + name_size])
            core = 1 if int(records[38 + name_size]) < 20 else 2

            return [name, state, u_jiffies, k_jiffies, core]

        start = time()

        threads = pd.read_csv(names[1], chunksize = 750000)

        df = []
        thread_df = []
        for thread in threads:
            thread = thread.dropna(subset = ['record']).reset_index(drop = True)
            thread_records = thread['record'].str.split(' ').map(parse_stats_records)
            df.append(thread_records)

            thread_df.append(thread.drop(columns = 'record'))

        thread = pd.concat(thread_df)
        df = pd.concat(df).dropna()

        thread['thread'] = df.map(lambda x: x[0])
        thread['state'] = df.map(lambda x: x[1])
        thread['u_jiffies'] = df.map(lambda x: x[2])
        thread['k_jiffies'] = df.map(lambda x: x[3])
        thread['socket'] = df.map(lambda x: x[4])
        thread.loc[thread['tid'] == pid, 'thread'] = 'main'

        # get differential jiffies
        thread['u_jiffies'] = thread.groupby('tid')['u_jiffies'].apply(pd.Series.diff)
        thread['k_jiffies'] = thread.groupby('tid')['k_jiffies'].apply(pd.Series.diff)
        thread['jiffies'] = thread['u_jiffies'] + thread['k_jiffies']
        thread['jiffies'] = thread['jiffies'].fillna(0)

        ########## JIFFIES PROCESSING ##########
        # separate each line into the desired pieces
        start = time()

        core_count = 40
        jiffy = jiffy['record'][jiffy['record'].str.contains(r'cpu\d+')]

        jiffy = jiffy.str.replace('  ', ' ')
        jiffy = jiffy.str.split(' ')
        jiffy = jiffy.map(filter_cpu).values.tolist()
        jiffy = pd.DataFrame(jiffy, columns = ['core'] + ['jiffies{}'.format(i) for i in range(10)], dtype = int)

        epochs = thread.epoch.unique()
        epochs = [40 * [epoch] for epoch in epochs]
        jiffy['epoch'] = [e for epoch in epochs for e in epoch]
        jiffy['socket'] = jiffy['core'].map(lambda x: 1 if x < 20 else 2)
        jiffy = jiffy.drop(columns = 'jiffies3')
        jiffy['jiffies'] = jiffy[[col for col in jiffy.columns if 'jiffies' in col]].sum(axis = 1)

        jiffy = jiffy.groupby(['epoch', 'socket'])['jiffies'].sum().reset_index()
        jiffy = df_diff(jiffy, 'socket', ['jiffies'])[['epoch', 'socket', 'jiffies']]

        print('{:.2f} seconds for jiffies'.format(time() - start))

        os_state = pd.merge(jiffy, thread.groupby(['epoch', 'socket'])['jiffies'].sum().reset_index(), on = ['epoch', 'socket'], suffixes = ('_', ''), how = 'outer')
        os_state['os_state'] = (os_state['jiffies'] / os_state['jiffies_']).fillna(0).replace(np.inf, 0)
        os_state.loc[os_state['os_state'] > 1, 'os_state'] = 1
        os_state = os_state[['epoch', 'socket', 'os_state']]

        thread = pd.merge(thread, os_state, on = ['epoch', 'socket'])

        print('{:.2f} seconds for os-thread'.format(time() - start))

        id = id[id['thread'] == id['thread']]
        id['thread'] = id['thread'].map(lambda x: x[:15])
        id['state'] = 1 * (id['state'] == 'RUNNABLE')
        thread = pd.merge(id, thread, on = ['epoch', 'thread'], how = 'outer', suffixes = ('', '_'))
        thread['state'] = thread['state'].fillna(thread['state_'])
        activity = activity['activeness'][activity.epoch.isin(thread.epoch)].replace('Infinity', 1).astype(float)
        thread.loc[thread['thread'] == 'Chaperone', 'state'] = activity.values

        thread = pd.merge(thread, thread.groupby(['epoch', 'socket'])['state'].sum().reset_index(), on = ['epoch', 'socket'], suffixes = ('', '_sum'))
        thread['os_state'] = thread['os_state'].fillna(method = 'bfill').fillna(0)
        thread['state'] = (thread['state'] / thread['state_sum']).fillna(0)
        thread['state'] *= thread['os_state']

        thread = thread[['epoch', 'timestamp', 'thread', 'id', 'tid', 'socket', 'state']]

        # compute the energy attribution
        thread = pd.merge(thread, energy, on = ['epoch', 'socket'])
        thread['package'] *= thread['state']
        thread['dram'] *= thread['state']
        thread = thread[['epoch', 'timestamp', 'thread', 'id', 'tid', 'socket', 'package', 'dram']]

        print('{:.2f} seconds for vm-thread'.format(time() - start))

        start = time()

        stack.columns = ['thread', 'timestamp', 'id', 'stack']
        stack = stack[(stack['timestamp'] >= thread['timestamp'].min()) & (stack['timestamp'] <= thread['timestamp'].max())]
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
        thread = thread.sort_values('epoch')

        print('{:.2f} seconds for methods'.format(time() - start))

        thread_path = os.path.join(args.destination, 'chappie.thread.{}.csv'.format(k))
        thread.to_csv(thread_path, index = False)

    # runtime_summary = pd.concat(runtime_summary)
    # runtime_summary.to_csv(os.path.join(args.destination, 'chappie.runtime.csv'.format(k)))
