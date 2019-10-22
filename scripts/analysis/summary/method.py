import os

import numpy as np
import pandas as pd

from tqdm import tqdm

from summary.component import JVM_JAVA

def filter_to_application(trace):
    while len(trace) > 0:
        record = trace[0]
        exclude = any((
            (r'java.' in record and '.java\.' not in record),
            (r'javax.' in record and '.javax\.' not in record),
            (r'jdk.' in record and '.jdk\.' not in record),
            (r'sun.' in record and '.sun\.' not in record),
            (r'org.apache.commons.' in record and '.org.apache.commons\.' not in record)
        ))
        if not exclude:
            return trace
        else:
            trace.pop(0)

    return 'end'

def method(path):
    df = pd.concat([pd.read_csv(os.path.join(path, f)) for f in np.sort(os.listdir(path))[2:]])
    df['energy'] = df.package + df.dram
    mask = df.name.str.contains('chappie') | df.name.isin(JVM_JAVA)
    df = df[~mask]

    df['filtered_trace'] = df.trace.str.split(';').map(filter_to_application)
    df['method'] = df.filtered_trace.str[0]

    df = df[df.method != 'end']
    df = df[~df.method.str.contains('glibc')]
    df = df[~df.method.str.contains('jrapl')]

    df = df.groupby('method').energy.agg(('sum', 'count'))
    df.columns = ['energy', 'time']
    df /= df.sum()

    return df
