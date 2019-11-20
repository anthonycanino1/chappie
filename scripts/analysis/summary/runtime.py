import json
import os
import re

import numpy as np
import pandas as pd

from tqdm import tqdm

import operator

_RAPL_WRAPAROUND = 16384

def rapl_wrap_around(reading):
    if reading >= 0:
        return reading
    else:
        return max(reading + _RAPL_WRAPAROUND, 0)

JVM_JAVA = (
    'Common-Cleaner',
    'Finalizer',
    'Java2D Disposer',
    'process reaper',
    'Reference Handl',
    'Reference Handler',
    'Signal Dispatch',
    'Signal Dispatcher'
)

JIT = ('(C\d CompilerThre)', )
GC = ('(G\d Conc#\d)', '(G\d Refine#\d)', '(G\d Young RemSet)', '(G\d Main Marker)', '(GC Thread#\d)')
OTHER = (
    'java',
    'Service Thread',
    'Sweeper thread',
    'VM Periodic Tas',
    'VM Thread'
)

JVM_C = re.compile('|'.join(JIT + GC + OTHER))

def thread_to_component(thread):
    if 'chappie' in thread or thread == 'Honest Profiler':
        return 'chappie'
    elif thread in JVM_JAVA:
        return 'jvm-java'
    elif JVM_C.match(thread) is not None:
        return 'jvm-c'
    else:
        return 'application'

def runtime(path):
    raw_root = os.path.join(path, 'raw')
    processed_root = os.path.join(path, 'processed')

    summary = []
    iters = np.sort([f for f in np.sort(os.listdir(raw_root)) if f != 'method.csv'])
    warm_up = len(iters) // 5
    iters = iters[warm_up:]

    for f in tqdm(iters):
        timestamps = [int(ts) for ts in json.load(open(os.path.join(raw_root, f, 'time.json'))).values()]
        duration = (max(timestamps) - min(timestamps)) / 1000

        if os.path.exists(os.path.join(raw_root, 'method.csv')):
            df = pd.read_csv(os.path.join(processed_root, 'method', '{}.csv'.format(f))).dropna()

            method = df.trace.str.split(';').apply(set).tolist()
            method = len({m for trace in method for m in trace})

            df = pd.read_csv(os.path.join(processed_root, 'energy', '{}.csv'.format(f))).dropna()
            df['component'] = df.name.apply(thread_to_component)
            df = df[(df.component == 'application') & (df.id > -1)]

            unique_threads = len(df.id.unique())
            live_threads = df.groupby('timestamp').id.count().max()

            summary.append(pd.Series(name = 'value', index = ['runtime', 'unique_threads', 'live_threads', 'sampled_methods'], data = [duration, unique_threads, live_threads, method]))
        else:
            summary.append(pd.Series(name = 'value', index = ['runtime',], data = [duration]))

    summary = pd.concat(summary).reset_index().groupby('index').agg(('mean', 'std'))['value']

    return summary
