import os
import json

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

def runtime(path):
    raw_root = os.path.join(path, 'raw')
    processed_root = os.path.join(path, 'processed')

    summary = []
    data = [f for f in np.sort(os.listdir(raw_root)) if f != 'method.csv'][2:]
    for f in tqdm(data):
        timestamps = [int(ts) for ts in json.load(open(os.path.join(raw_root, f, 'time.json'))).values()]
        runtime = max(timestamps) - min(timestamps)

        energy = pd.read_csv(os.path.join(raw_root, f, 'energy.csv'), delimiter = ';')
        energy['energy'] = energy.package + energy.dram
        energy = energy.groupby('socket').energy.diff().apply(rapl_wrap_around).sum()

        if os.path.exists(os.path.join(raw_root, 'method.csv')):
            df = pd.read_csv(os.path.join(processed_root, 'method', '4', '{}.csv'.format(f))).dropna()

            thread = df.groupby('timestamp').id.agg(('unique', 'count'))
            thread['unique'] = thread['unique'].apply(len)

            method = df.trace.str.split(';').apply(set).tolist()
            method = len({m for trace in method for m in trace})

            summary.append(pd.Series(name = 'value', index = ['runtime', 'energy', 'unique_threads', 'live_threads', 'sampled_methods'], data = [runtime, energy, thread['unique'].max(), thread['count'].max(), method]))
        else:
            summary.append(pd.Series(name = 'value', index = ['runtime', 'energy'], data = [runtime, energy]))

    summary = pd.concat(summary).reset_index().groupby('index').agg(('mean', 'std'))['value']

    return summary
