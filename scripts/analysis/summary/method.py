import os

import pandas as pd

from tqdm import tqdm

def method(path):
    df = pd.concat(tqdm(pd.read_csv(os.path.join(path, f)) for f in os.listdir(path)))
    df['energy'] = df.package + df.dram
    df = df.groupby('trace').energy.agg(('sum', 'count'))
    df.columns = ['energy', 'hits']

    return df
