import os

import numpy as np
import pandas as pd

from tqdm import tqdm

from summary.component import JVM_JAVA

def filter_to_application_(trace):
    try:
        while len(trace) > 0:
            record = trace[0]
            exclude = any((
                (r'.' not in record),
                (r'java.' in record and '.java\.' not in record),
                (r'javax.' in record and '.javax\.' not in record),
                (r'jdk.' in record and '.jdk\.' not in record),
                (r'sun.' in record and '.sun\.' not in record),
                (r'org.apache.commons.' in record and '.org.apache.commons\.' not in record),
                (r'<init>' in record)
            ))
            if not exclude:
                return trace
            else:
                trace.pop(0)
    except:
        pass

    return 'end'

def filter_to_application(df):
    mask = (df.trace == 'end') | df.trace.str.contains('chappie') | df.trace.str.contains('jlibc') | df.trace.str.contains('jrapl') | df.name.isin(JVM_JAVA)
    df = df[~mask]
    df.trace = df.trace.str.split(';').map(filter_to_application_)
    method = df.trace.str[0]
    df = df[(df.trace != 'end') & (method != 'e') & ~(method.str.contains('org.dacapo.harness'))]

    return df

def method(path):
    try:
        iters = [f for f in np.sort(os.listdir(path)) if '.csv' in f]
        warm_up = len(iters) // 5
        df = pd.concat([pd.read_csv(os.path.join(path, f)).assign(iter = i) for i, f in enumerate(tqdm(iters))])
    except:
        iters = np.sort(os.listdir(path + '/0'))
        warm_up = len(iters) // 5
        df = pd.concat([pd.read_csv(os.path.join(path, '0', f)).assign(iter = i) for i, f in enumerate(tqdm(iters))])

    df = filter_to_application(df)
    df.trace = df.trace.str.join(';')
    df['energy'] = df.package + df.dram

    df = df.groupby(['trace', 'iter']).energy.agg(('sum', 'count'))
    df.columns = ['energy', 'time']

    return df
