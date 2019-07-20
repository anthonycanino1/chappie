#!/usr/bin/python3

import os
import subprocess

from io import BytesIO

import numpy as np
import pandas as pd

from tqdm import tqdm

_RAPL_WRAPAROUND = 16384

def rapl_wrap_around(reading):
    if reading >= 0:
        return reading
    else:
        return max(reading + _RAPL_WRAPAROUND, 0)

def process_energy(path, iters):
    energy = pd.read_csv(os.path.join(path, 'chappie.energy.{}.csv'.format(iters))).sort_values('epoch')

    energy.package = energy.groupby('socket').package.diff()
    energy.dram = energy.groupby('socket').dram.diff()

    energy.package = energy.package.map(rapl_wrap_around)
    energy.dram = energy.dram.map(rapl_wrap_around)
    energy = energy.fillna(0)

    return energy

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

def process_os_threads(path, iters, main_id):
    os_thread = pd.read_csv(os.path.join(path, 'chappie.thread.{}.csv'.format(iters))).sort_values('epoch')

    os_thread = os_thread.dropna(subset = ['record'])
    records = os_thread.record.str.split(' ').map(parse_stats_record)

    os_thread.loc[:, 'os_thread'] = records.map(lambda x: x[0])
    os_thread.loc[os_thread.tid == main_id, 'os_thread'] = 'main'

    os_thread.loc[:, 'state'] = records.map(lambda x: x[1])
    os_thread.loc[:, 'jiffies'] = records.map(lambda x: x[2])
    os_thread.loc[:, 'socket'] = records.map(lambda x: x[3])
    os_thread.pop('record')

    # os_names = []
    # for _, df in os_thread.groupby(['epoch', 'thread']):
    #     if len(df) > 1:
    #         names = df.loc[:, 'thread'] + [str(i) for i in range(len(df))]
    #     else:
    #         names = df.thread
    #     os_names.append(names)
    # os_thread['os_thread'] = pd.concat(os_names)

    os_thread.jiffies = os_thread.groupby('tid').jiffies.diff().fillna(0)

    return os_thread

def process_vm_threads(path, iters):
    vm_thread = pd.read_csv(os.path.join(path, 'chappie.id.{}.csv'.format(iters))).sort_values('epoch')
    tid_map = pd.read_csv(os.path.join(path, 'chappie.tid.{}.csv'.format(iters))).set_index('thread').to_dict()['tid']
    vm_thread['tid'] = vm_thread.thread.map(tid_map).fillna(-1).astype(int)

    vm_thread['os_thread'] = vm_thread[vm_thread.tid == -1].thread.map(lambda x: x[:15])
    vm_thread['os_thread'] = vm_thread['os_thread'].fillna('-1')

    # os_names = []
    # for _, df in vm_thread.groupby(['epoch', 'os_thread']):
    #     if len(df) > 1:
    #         names = df.loc[:, 'os_thread'] + [str(i) for i in range(len(df))]
    #     else:
    #         names = df.thread
    #     os_names.append(names)
    # vm_thread.os_thread = pd.concat(os_names)
    # print(len(vm_thread))

    vm_thread.state = vm_thread.state.astype(float)

    chappie = pd.read_csv(os.path.join(path, 'chappie.activeness.{}.csv'.format(iters))).sort_values('epoch')

    vm_thread.loc[vm_thread.thread == 'Chaperone', 'state'] = chappie.loc[chappie.epoch.isin(vm_thread.epoch), 'activeness']
    vm_thread.loc[vm_thread.thread == 'Chaperone', 'state'] = vm_thread.loc[vm_thread.thread == 'Chaperone', 'state'].fillna(method = 'ffill').fillna(0)

    return vm_thread

def process_system(path, iters, thread):
    system = pd.read_csv(os.path.join(path, 'chappie.system.{}.csv'.format(iters)))
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
    epochs = thread.epoch.unique()
    epochs = [cores * [epoch] for epoch in epochs]
    system['epoch'] = [e for epoch in epochs for e in epoch][:len(system)]

    system = system.groupby(['epoch', 'socket'])['jiffies'].sum().reset_index()
    system.jiffies = system.groupby(['socket']).jiffies.diff().fillna(0)

    return system

def attribute_energy(os, vm, system, energy):
    # print('hello?')

    thread = os.groupby(['epoch', 'socket']).jiffies.sum()
    os_state = pd.merge(system, thread, on = ['epoch', 'socket'], suffixes = ('_', ''), how = 'outer')
    os_state['state'] = os_state.jiffies / os_state.jiffies_
    os_state.state = os_state.state.replace(np.inf, 0).fillna(0).clip(0, 1)
    os_state = os_state.drop(columns = [col for col in os_state.columns if '_' in col])
    os_state = pd.merge(os, os_state, on = ['epoch', 'socket'], suffixes = ('_', ''), how = 'left')
    os_state['thread'] = os_state.os_thread
    # print(os_state)
    os_state = os_state.drop(columns = [col for col in os_state.columns if '_' in col])
    # print(os_state)

    # print(vm)
    # print(os)
    tid_state = pd.merge(vm.drop(columns = 'timestamp'), os, on = ['epoch', 'tid'], suffixes = ('_', ''), how = 'left')
    tid_state.state = tid_state.state_.fillna(tid_state.state)
    tid_state = tid_state.dropna()
    # tid_state = tid_state.drop(columns = [col for col in tid_state.columns if '_' == col[-1]]).dropna()

    name_state = pd.merge(os, vm.drop(columns = 'timestamp'), on = ['epoch', 'os_thread'], suffixes = ('_', ''), how = 'left')
    # print('?')
    name_state.thread = name_state.thread.fillna(name_state.os_thread)
    name_state.tid = name_state.tid.fillna(-1).astype(int)
    name_state = name_state[name_state.tid == -1]
    name_state.id = name_state.id.fillna(-1).astype(int)
    name_state.state = name_state.state.fillna(name_state.state_)
    name_state = name_state.dropna()
    # name_state = name_state.drop(columns = [col for col in name_state.columns if '_' == col[-1]]).dropna()

    # print(tid_state)
    # print(name_state)
    vm_state = pd.concat([tid_state, name_state], sort = True)
    vm_state.thread = vm_state.thread.fillna(vm_state.os_thread)
    vm_state = vm_state.drop(columns = [col for col in vm_state.columns if '_' in col])
    # print(vm_state)
    # sys.exit(0)
    # sys.exit(0)
    # thread.state *= thread.state_
    # print(vm_state.thread.unique())
    # print(vm_state.os_thread.unique())
    # sys.exit(0)
    # vm_state.thread = vm_state.thread.fillna(vm_state.os_thread)

    # print(vm_state)

    vm_state = pd.merge(vm_state, vm_state.groupby(['epoch', 'socket']).state.sum().reset_index(), on = ['epoch', 'socket'], suffixes = ('', '_sum'))
    vm_state.state = (vm_state.state / vm_state.state_sum)
    vm_state.state = vm_state.state.replace(np.inf, 0).fillna(0).clip(0, 1)
    vm_state = vm_state.drop(columns = [col for col in vm_state.columns if '_' in col])

    # print(vm_state)
    # print(os_state)
    thread = pd.merge(vm_state, os_state, on = ['epoch', 'thread'], how = 'outer', suffixes = ('', '_'))
    thread.state *= thread.state_

    os_rate = thread.epoch.unique().min()
    energy['epoch'] = energy.epoch // os_rate
    energy = energy.groupby(['epoch', 'socket']).sum().reset_index()
    energy.epoch *= os_rate

    thread = pd.merge(thread, energy, on = ['epoch', 'socket'], how = 'outer', suffixes = ('', '_')).dropna()
    thread.package *= thread.state
    thread.dram *= thread.state

    thread['id'] = thread['id'].fillna(-1).astype(int)
    thread['tid'] = thread['tid'].astype(int)
    thread['socket'] = thread['socket'].astype(int)

    return thread[['epoch', 'timestamp', 'thread', 'id', 'tid', 'socket', 'package', 'dram']]

_ALIGN_SCRIPT = os.path.realpath(os.path.join(os.path.dirname(__file__), 'opt-align.rb'))
ALIGN_METHODS = ['ruby', _ALIGN_SCRIPT, '-c', 'thread', '-h', 'stack']

def align_methods(path, thread):
    temp_path = os.path.join(path, 'temp.thread')
    thread.to_csv(temp_path, index = False)
    ALIGN_METHODS[3] = temp_path

    ALIGN_METHODS[5] = os.path.join(path, 'chappie.stack.csv')

    thread = subprocess.check_output(ALIGN_METHODS)
    thread = pd.read_csv(BytesIO(thread))

    os.remove(temp_path)

    return thread

def attribute(path, iters, main_id):
    steps = tqdm(iters, total = 7)
    for i in steps:
        try:
            steps.reset()
            steps.set_description('iter {}'.format(i + 1))

            energy = process_energy(path, i)
            steps.update(1)

            os_threads = process_os_threads(path, i, main_id)
            steps.update(1)

            vm = process_vm_threads(path, i)
            steps.update(1)

            system = process_system(path, i, os_threads)
            steps.update(1)

            # print('?')

            thread = attribute_energy(os_threads, vm, system, energy)
            steps.update(1)

            thread = align_methods(path, thread)
            steps.update(1)

            # print('?')

            thread.to_csv(os.path.join(path, 'processed', 'chappie.thread.{}.csv'.format(i)), index = False)
            steps.update(1)
        except:
            raise
            pd.DataFrame(columns = ['epoch', 'timestamp', 'thread', 'id', 'tid', 'socket', 'package', 'dram', 'stack']).to_csv(os.path.join(path, 'processed', 'chappie.thread.{}.csv'.format(i)), index = False)
