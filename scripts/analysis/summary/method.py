import os

import numpy as np
import pandas as pd

from tqdm import tqdm

from summary.component import JVM_JAVA

def filter_to_application(trace):
    try:
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
    except:
        pass

    return 'end'

def method(path):
    df = pd.concat([pd.read_csv(os.path.join(path, f)).assign(iter = i) for i, f in enumerate(tqdm(np.sort(os.listdir(path))[2:]))])
    df['energy'] = df.package + df.dram
    mask = df.name.str.contains('chappie') | df.trace.str.contains('chappie') | df.trace.str.contains('jlibc') | df.trace.str.contains('jrapl') | df.name.isin(JVM_JAVA)
    df = df[~mask]

    df['trace'] = df.trace.str.split(';').map(filter_to_application).str.join(';')
    df = df[(df.trace != 'end') | (df.trace != 'e;n;d')]
    df = df.groupby('trace').energy.agg(('sum', 'count'))
    df.columns = ['energy', 'time']

    return df
