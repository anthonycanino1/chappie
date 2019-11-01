import os
import re

import numpy as np
import pandas as pd

from tqdm import tqdm

JVM_JAVA = (
    'Common-Cleaner',
    'Finalizer',
    'Java2D Disposer',
    'process reaper',
    'Reference Handler',
    'Signal Dispatcher'
)

JIT = ('CompilerThre', )
GC = ('Main Marker', 'Marker#', 'Refine#', 'Young RemSet', 'GC Thread#')
OTHER = (
    'java',
    'Service Thread',
    'Sweeper thread',
    'VM Periodic Tas',
    'VM Thread'
)

JVM_C = re.compile('|'.join(JIT + GC + OTHER))

def thread_to_component(thread):
    if 'chappie' in thread or thread is 'Honest Profiler':
        return 'chappie'
    elif thread in JVM_JAVA:
        return 'jvm-java'
    elif JVM_C.match(thread) is not None:
        return 'jvm-c'
    else:
        return 'application'

def component(path):
    df = pd.concat(tqdm(pd.read_csv(os.path.join(path, f)).assign(iter = k) for k, f in enumerate(np.sort(os.listdir(path))[2:])))

    runtime = df.groupby('iter').timestamp.agg(('min', 'max'))
    runtime['runtime'] = runtime['max'] - runtime['min']
    runtime = runtime[['runtime']].agg(('mean', 'std'))

    df['component'] = df.name.map(thread_to_component)
    df = df.groupby(['socket', 'component', 'iter'])[['package', 'dram']].sum()
    df = df.groupby(['socket', 'component'])[['package', 'dram']].mean()

    return runtime, df
