#!/usr/bin/python3

import argparse
import json
import os
import re
import sys

from time import time
import xml.etree.ElementTree as ET

import numpy as np
import pandas as pd

from tqdm import tqdm

def runtime_summary(root):
    runtime = []
    root_ = os.path.join(root, '1_{}'.format(root.split('/')[-1]))
    if os.path.exists(root_):
        root = root_

    for path in tqdm(os.listdir(root)):
        # benchmark = os.path.basename(root).split('_')[1]
        if path not in ('summary', 'plots'):
            df = pd.read_csv(os.path.join(root, path, 'processed', 'chappie.runtime.csv'), index_col = 'iteration')
            df['experiment'] = os.path.basename(path)
            # df['benchmark'] = benchmark
            runtime.append(df)

    runtime = pd.concat(runtime, sort = True).groupby('experiment').agg(('mean', 'std'))
    try:
        runtime.columns = ['energy', 'energy_std', 'rate', '', 'rate_std', '', 'runtime', 'runtime_std']
    except:
        runtime.columns = ['energy', 'energy_std', 'runtime', 'runtime_std']
    runtime = runtime.drop(columns = '', errors = 'ignore')

    time_ref = runtime.loc['NOP', 'runtime']
    runtime['overhead'] = (runtime['runtime'] - time_ref) / time_ref
    runtime['overhead_std'] = 1 + (runtime['runtime_std'] - time_ref) / time_ref

    runtime['overhead'] *= 100
    runtime['overhead_std'] *= 100

    return runtime

def attribution_summary(root):
    attribution = []
    root_ = os.path.join(root, '1_{}'.format(root.split('/')[-1]))
    if os.path.exists(root_):
        root = root_

    for path in tqdm(os.listdir(root)):
        if path not in ('summary', 'plots', 'NOP'):
            files = [f for f in os.listdir(os.path.join(root, path, 'processed')) if 'thread' in f]
            dfs = []
            for k, f in enumerate(files):
                df = pd.read_csv(os.path.join(root, path, 'processed', f))
                df['iteration'] = k
                dfs.append(df)
            df = pd.concat(dfs).groupby(['iteration']).sum().reset_index()
            df['benchmark'] = os.path.basename(path)

            attribution.append(df)

    attribution = pd.concat(attribution).groupby('benchmark')[['package', 'dram']].agg(('mean', 'std')).sum(axis = 1)
    return attribution

JIT = ('CompilerThre', )
GC = ('Main Marker', 'Marker#', 'Refine#', 'Young RemSet', 'GC Thread#')
OTHER = (
    'Service Thread',
    'Sweeper thread',
    'VM Periodic Tas',
    'VM Thread'
)

JVM_C = re.compile('|'.join(JIT + GC + OTHER))

JVM = (
    'Common-Cleaner',
    'Reference Handl',
    'Signal Dispatch',
    'Finalizer',
    'process reaper'
)

CHAPPIE = (
    'Chaperone',
    'Honest Profiler'
)

def thread_to_component(thread):
    if thread in CHAPPIE:
        return 'chappie'
    elif thread in JVM:
        return 'jvm-java'
    elif JVM_C.match(thread) is not None:
        return 'jvm-c'
    else:
        return 'application'

def component_summary(root):
    component = []
    root_ = os.path.join(root, '1_{}'.format(root.split('/')[-1]))
    if os.path.exists(root_):
        root = root_

    for path in tqdm(os.listdir(root)):
        try:
            if path not in ('summary', 'plots', 'NOP'):
                files = [f for f in os.listdir(os.path.join(root, path, 'processed')) if 'thread' in f]
                dfs = []
                for k, f in enumerate(files):
                    df = pd.read_csv(os.path.join(root, path, 'processed', f))
                    df['iteration'] = k
                    dfs.append(df)
                df = pd.concat(dfs)
                df['benchmark'] = os.path.basename(path)
                df['component'] = df.thread.map(thread_to_component)
                component.append(df)
        except:
            pass

    component = pd.concat(component)
    component = component.groupby(['benchmark', 'iteration', 'socket', 'component'])[['package', 'dram']].sum()
    component = component.groupby(['benchmark', 'socket', 'component'])[['package', 'dram']].mean()

    return component

def method_summary(root):
    method = []
    root_ = os.path.join(root, '1_{}'.format(root.split('/')[-1]))
    if os.path.exists(root_):
        root = root_

    for path in tqdm(os.listdir(root)):
        if path not in ('summary', 'plots', 'NOP'):
            files = [f for f in os.listdir(os.path.join(root, path, 'processed')) if 'thread' in f]
            df = pd.concat([pd.read_csv(os.path.join(root, path, 'processed', f)) for f in files])
            df = df[(df['stack'] != 'end') & (df.thread != 'Chaperone')]
            df['benchmark'] = os.path.basename(path)

            method.append(df)

    method = pd.concat(method)
    method['energy'] = method[['package', 'dram']].sum(axis = 1)
    method = method[method['stack'] != 'end'].groupby(['benchmark', 'stack'])['energy'].agg(('sum', 'count'))
    method.columns = ['energy', 'hits']
    method = method.sort_values(by = 'energy', ascending = False).reset_index()
    method = method.sort_values(['energy', 'benchmark'], ascending = False)

    return method
