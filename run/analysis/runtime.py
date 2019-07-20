import os

import numpy as np
import pandas as pd

_RAPL_WRAPAROUND = 16384

def rapl_wrap_around(reading):
    if reading >= 0:
        return reading
    else:
        return max(reading + _RAPL_WRAPAROUND, 0)

def process_runtime(path, iters):
    files = np.sort([f for f in os.listdir(path) if 'runtime' in f and any(str(i) in f for i in iters)])

    runtime = pd.concat([pd.read_csv(os.path.join(path, f)) for f in files]).reset_index()
    runtime['iteration'] = runtime.index // 6
    runtime = runtime.pivot(index ='iteration', columns = 'name', values = 'value')
    runtime['runtime'] /= 1000
    for col in ('package1', 'package2', 'dram1', 'dram2'):
        runtime[col] = runtime[col].map(rapl_wrap_around)
    runtime['energy'] = runtime[['package1', 'package2', 'dram1', 'dram2']].sum(axis = 1)

    runtime['main_id'] = runtime['main_id'].astype(int)
    runtime[['energy', 'runtime']].to_csv(os.path.join(path, 'processed', 'chappie.runtime.csv'), header = True)

    main_id = runtime['main_id'].max().max()
    return main_id

def process_rates(path, iters):
    files = np.sort([f for f in os.listdir(path) if 'activeness' in f and any(str(i) in f for i in iters)])
    rate = pd.concat([pd.read_csv(os.path.join(path, f))['total'].agg(('mean', 'std')) for f in files], axis = 1).T.reset_index()

    runtime = pd.read_csv(os.path.join(path, 'processed', 'chappie.runtime.csv'), index_col = 'iteration')
    runtime['rate'] = rate['mean']
    runtime['rate_std'] = rate['std']

    runtime.to_csv(os.path.join(path, 'processed', 'chappie.runtime.csv'), header = True)
