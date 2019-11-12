import os

import matplotlib.pyplot as plt
import numpy as np
import pandas as pd

from tqdm import tqdm

def cfa(path):
    method = pd.read_csv(os.path.join(path, 'method.csv'))
    method['method'] = method.trace.str.split(';').str[0].str.split('.').str[-2:].str.join('.')

    top_methods = method.groupby('method').energy.sum().sort_values(ascending = False).head(3).index.values

    context1 = method.trace.str.split(';').str[1].str.split('.').str[-2:].str.join('.')
    context2 = method.trace.str.split(';').str[2].str.split('.').str[-2:].str.join('.')
    method['context'] = "[" + context1 + ", " + context2 + "]"

    method = method[method.method.isin(top_methods)].groupby(['method', 'context']).energy.sum()
    method /= method.groupby('method').sum()

    for i, (m, df) in enumerate(method.reset_index().groupby('method')):
        ax = df.plot(kind = 'pie', x = 'context', y = 'energy', wedgeprops = {'edgecolor': 'k', 'linewidth': 1}, labels = None, figsize = (24, 16))

        plt.title(m, fontsize = 72)
        plt.xlabel('')
        plt.ylabel('')

        if len(df.context) < 10:
            ax.legend(df.context, loc = 'upper left', bbox_to_anchor = (0.925, 0.925), frameon = False, fontsize = 48)
        else:
            ax.legend(df.context, fontsize = 10, ncol = int(np.ceil(len(df.context) / 30)))

        plt.savefig(os.path.join(path, '..', 'plots', 'top_{}_cfa2.pdf'.format(i + 1)), bbox_inches = 'tight', legend = True)
