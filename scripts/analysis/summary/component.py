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
    iters = np.sort(os.listdir(path))
    df = pd.concat(tqdm(pd.read_csv(os.path.join(path, f)).assign(iter = k) for k, f in enumerate(iters)))

    df['component'] = df.name.map(thread_to_component)
    df = df.groupby(['socket', 'component', 'iter'])[['package', 'dram']].sum()
    df = df.groupby(['socket', 'component'])[['package', 'dram']].mean()

    return df.sort_index()
