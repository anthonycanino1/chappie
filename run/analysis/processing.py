#!/usr/bin/python3

import argparse
import json
import os
import re
import subprocess

from csv import writer
from io import BytesIO, StringIO
from time import time
import xml.etree.ElementTree as ET

import numpy as np
import pandas as pd

from tqdm import tqdm

def parse_args():
    parser = argparse.ArgumentParser()
    parser.add_argument('-benchmark')
    parser.add_argument('-config')

    return parser.parse_args()

def parse_config(benchmark, config):
    benchmark = json.load(open(benchmark, 'r'))

    root = ET.parse(config).getroot()
    for child in root:
        try:
            benchmark[child.tag] = int(child.text)
        except:
            benchmark[child.tag] = child.text

    return benchmark

rapl_wrap_around_value = 16384

def rapl_wrap_around(x):
    if x >= 0:
        return x
    else:
        return max(x + rapl_wrap_around_value, 0)

def parse_stats_record(record):
    # unix stat records have 52 items but
    # both the items and java thread names
    # are space delimited; therefore, we
    # need to adjust our index to match
    # the extra characters
    name_length = len(record) - 52
    idx = lambda x: x + name_length

    name = ' '.join(record[1:idx(2)])[1:-1]
    state = 1 if record[idx(2)] == 'R' else 0
    jiffies = int(record[idx(13)]) + int(record[idx(14)])
    core = 1 if int(record[idx(38)]) < 20 else 2

    return [name, state, jiffies, core]

method_align_file = os.path.realpath(os.path.join(os.path.dirname(__file__), 'opt-align.rb'))
align_args = ["ruby", method_align_file, '-c', 'thread', '-h' , 'stack']

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
    args = parse_args()
    config = parse_config(args.benchmark, args.config)

    args.path = config['workPath']

    # setup the paths
    if not os.path.exists(args.path):
        raise FileNotFoundError('No directory at {}'.format(os.path.abspath(args.path)))

    os.chdir(args.path)
    if not os.path.exists('processed'):
        os.mkdir('processed')

    iters = range(config['iters'] // config['warm_up_fraction'], config['iters'])
    stack = None
    if os.path.exists('chappie.stack.csv'):
        stack = pd.read_csv('chappie.stack.csv')

    runtime_summary = []

    for i in iters:
        start = time()

        # for f in os.listdir():
            # if 'csv' in f and '.{}.'.format(i) in f:
                # print(f)
                # pd.read_csv(f)
        df = pd.read_csv('chappie.activeness.{}.csv'.format(i))
        df.timestamp = df.timestamp.diff()
        print(df.describe()['timestamp'])
        continue

        data = {f.split(r'.')[1]: pd.read_csv(f) for f in os.listdir() if 'csv' in f and '.{}.'.format(i) in f}

        # print(data['activeness'].describe())
        # print(len(data['activeness'][data['activeness']['total'] == 10]), len(data['activeness']), len(data['activeness'][data['activeness']['total'] == 10]) / len(data['activeness']))

        import sys
        sys.exit()

        # print('{:.2f} seconds for loading'.format(time() - start))

        # RUNTIME #
        start = time()

        # df = data['misses']
        # df['dt'] = df.epoch.diff().dropna().astype(int)
        # df.dt.value_counts().sort_index().to_csv(os.path.join('processed', 'chappie.miss.epoch.{}.csv'.format(i)), header = True)
        #
        # # df = data['misses']
        # df['dt'] = (df.timestamp.diff() / df.epoch.diff()).dropna().astype(int)
        # df.dt.value_counts().sort_index().to_csv(os.path.join('processed', 'chappie.miss.timestamp.{}.csv'.format(i)), header = True)

        # df = data['misses']
        # df.drop(columns = 'activeness').dropna().astype(int).set_index('epoch').to_csv(os.path.join('processed', 'chappie.frame.{}.csv'.format(i)))
        # continue

        # df['dt'] = (df.timestamp.diff() / df.epoch.diff()).dropna().astype(int)
        # df.dt.value_counts().sort_index().to_csv(os.path.join('processed', 'chappie.active.epoch.{}.csv'.format(i)), header = True)
        #
        # df['dt'] = df.epoch.diff().dropna().astype(int)
        # df.dt.value_counts().sort_index().to_csv(os.path.join('processed', 'chappie.active.timestamp.{}.csv'.format(i)), header = True)

        # print(data['activeness'].activeness.max())

        df = data['runtime'].append(pd.DataFrame({
            'name': ['active', 'missed'],
            'value': [len(data['activeness']), len(data['misses'])]
        }))
        # print(data['runtime'])
        # print(data['activeness'].count())
        # print(data['misses'].count())

        runtime_summary.append(df) # data['runtime'])
        continue
        main_id = data['runtime'].set_index('name').to_dict()['value']['main_id']

        print('{:.2f} seconds for runtime'.format(time() - start))

        # ENERGY #
        start = time()

        energy = data['energy'].sort_values('epoch')

        energy.package = energy.groupby('socket').package.diff()
        energy.dram = energy.groupby('socket').dram.diff()

        energy.package = energy.package.map(rapl_wrap_around)
        energy.dram = energy.dram.map(rapl_wrap_around)
        energy = energy.fillna(0)

        energy.to_csv(os.path.join('processed', 'chappie.energy.{}.csv'.format(i)), index = False)

        print('{:.2f} seconds for energy'.format(time() - start))

        start = time()

        os_thread = data['thread'].sort_values('epoch')

        os_thread = os_thread.dropna(subset = ['record'])
        records = os_thread.record.str.split(' ').map(parse_stats_record)

        os_thread.loc[:, 'thread'] = records.map(lambda x: x[0])
        os_thread.loc[:, 'state'] = records.map(lambda x: x[1])
        os_thread.loc[:, 'jiffies'] = records.map(lambda x: x[2])
        os_thread.loc[:, 'socket'] = records.map(lambda x: x[3])
        os_thread.pop('record')

        os_thread.jiffies = os_thread.groupby('tid').jiffies.diff().fillna(0)

        os_thread.loc[os_thread.tid == main_id, 'thread'] = 'main'

        print('{:.2f} seconds for os threads'.format(time() - start))

        start = time()

        vm_thread = data['id'].copy(deep = True)

        vm_thread.thread = vm_thread.thread.map(lambda x: x[:15])
        vm_thread.state = vm_thread.state.astype(float)

        chappie = data['activeness']

        vm_thread.loc[vm_thread.thread == 'Chaperone', 'state'] = chappie.loc[chappie.epoch.isin(vm_thread.epoch), 'activeness']

        print('{:.2f} seconds for vm threads'.format(time() - start))

        start = time()

        system = data['system']
        system = system.record[system.record.str.contains(r'cpu\d+')]

        system = system.str.replace('  ', ' ')
        system = system.str.split(' ')
        system = system.values.tolist()
        system = pd.DataFrame(system, columns = ['core'] + ['jiffies{}'.format(i) for i in range(len(system[0]) - 1)], dtype = int)

        system.core = system.core.str.replace('cpu', '')
        system = system.astype(int)
        system['socket'] = system.core.map(lambda c: 1 if c < 20 else 2)
        system.pop('jiffies3')

        system['jiffies'] = system[[col for col in system.columns if 'core' not in col]].sum(axis = 1).astype(int)

        cores = len(system.core.unique())
        epochs = data['thread'].epoch.unique()
        epochs = [cores * [epoch] for epoch in epochs]
        l1 = len(system)
        l2 = len([e for epoch in epochs for e in epoch])
        system['epoch'] = [e for epoch in epochs for e in epoch] #[:(l1 - l2)]

        system = system.groupby(['epoch', 'socket']).sum().reset_index()
        system['jiffies'] = system[[col for col in system.columns if 'jiffies' in col]].sum(axis = 1)
        system.jiffies = system.groupby(['socket']).jiffies.diff().fillna(0)

        print('{:.2f} seconds for system'.format(time() - start))

        start = time()

        thread = os_thread.groupby(['epoch', 'socket']).jiffies.sum()
        os_state = pd.merge(system, thread, on = ['epoch', 'socket'], suffixes = ('_', ''), how = 'outer')
        os_state['state'] = os_state.jiffies / os_state.jiffies_
        os_state.state = os_state.state.replace(np.inf, 0).fillna(0)
        os_state = os_state.drop(columns = [col for col in os_state.columns if '_' in col])

        print('{:.2f} seconds for os state'.format(time() - start))

        start = time()

        vm_state = pd.merge(os_thread, vm_thread.drop(columns = 'timestamp'), on = ['epoch', 'thread'], suffixes = ('_', ''), how = 'outer')
        vm_state.id = vm_state.id.fillna(-1).astype(int)
        vm_state.state = vm_state.state.fillna(vm_state.state_)

        vm_state = pd.merge(vm_state, vm_state.groupby(['epoch', 'socket']).state.sum(), on = ['epoch', 'socket'], suffixes = ('', '_sum'))
        vm_state.state = (vm_state.state / vm_state.state_sum).fillna(0)

        vm_state = vm_state.drop(columns = [col for col in vm_state.columns if '_' in col])

        print('{:.2f} seconds for vm state'.format(time() - start))

        start = time()

        thread = pd.merge(vm_state, os_state, on = ['epoch', 'socket'], how = 'outer', suffixes = ('', '_'))
        thread.state *= thread.state_

        thread = thread.drop(columns = [col for col in thread.columns if '_' in col])

        print('{:.2f} seconds for total state'.format(time() - start))

        start = time()

        thread = pd.merge(thread, energy, on = ['epoch', 'socket'], how = 'outer', suffixes = ('', '_')).dropna()
        thread.package *= thread.state
        thread.dram *= thread.state

        thread = thread[['epoch', 'timestamp', 'thread', 'id', 'tid', 'socket', 'package', 'dram']]

        print('{:.2f} seconds for attribution'.format(time() - start))

        start = time()

        thread['id'] = thread['id'].astype(int)

        thread.to_csv('thread', index = False)
        if stack is not None:
            data['stack'] = stack
        data['stack'].to_csv('stack', index = False)

        thread = subprocess.check_output(align_args)
        thread = pd.read_csv(BytesIO(thread))

        thread['all'] = thread['stack'].str.split(';').map(lambda x: 'end' if 'java' in x[0] else ';'.join(x))
        thread['unfiltered'] = thread['stack']
        thread['deep'] = thread['stack'].str.split(';').map(filter_to_application)

        thread = thread.drop(columns = ['stack']).sort_values('epoch')
        thread = thread.sort_values('epoch')

        print('{:.2f} seconds for method alignment'.format(time() - start))

        start = time()

        thread.to_csv(os.path.join('processed', 'chappie.thread.{}.csv'.format(i)), index = False)

        print('{:.2f} seconds for writing'.format(time() - start))

    import sys
    sys.exit()

    if os.path.exists('thread'):
        os.remove('thread')
        os.remove('stack')

    start = time()

    runtime_summary = pd.concat(runtime_summary)
    runtime_summary.to_csv(os.path.join('processed', 'chappie.runtime.csv'), index = False)

    # print('{:.2f} seconds for writing runtime'.format(time() - start))
