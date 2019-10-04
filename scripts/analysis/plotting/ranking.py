import os

import matplotlib.pyplot as plt
import numpy as np
import pandas as pd

from tqdm import tqdm

def ranking(path):
    method = pd.read_csv(os.path.join(path, 'method.csv'))

    for k, df in tqdm(method.groupby('k')):
        df.loc[:, 'package'] = df.method.str.split('.').str[:-2].str.join('.')
        df.loc[:, 'class'] = df.method.str.split('.').str[-2]
        df.loc[:, 'method'] = df.method.str.split('.').str[-2:].str.join('.')

        for col in ('method', 'class', 'package'):
            df_ = df.groupby(col).sum().sort_values('energy')
            if col == 'method':
                df_.tail(10).plot(kind = 'barh', y = ['energy', 'time'], width = 0.3)
            else:
                df_.tail(10).plot(kind = 'barh', y = 'energy', width = 0.3)
            plt.savefig(os.path.join(path, '..', 'plots', 'k={}_{}_ranking.pdf'.format(int(k) if not k == np.inf else k, col)), bbox_inches = 'tight')
            plt.close()
