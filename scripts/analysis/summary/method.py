import os

import pandas as pd

from tqdm import tqdm

from summary.component import JVM_JAVA

def method(path):
    df = pd.concat(tqdm(pd.read_csv(os.path.join(path, f)) for f in os.listdir(path)))
    df['energy'] = df.package + df.dram
    df = df[~(df.name.str.contains('chappie')) & ~(df.name.isin(JVM_JAVA))]

    df = df.groupby('trace').energy.agg(('sum', 'count'))
    df.columns = ['energy', 'hits']

    # need to strip the trace still

    return df
