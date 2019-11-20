import os

import matplotlib.pyplot as plt
import numpy as np
import pandas as pd
import seaborn as sns

from tqdm import tqdm

def correlation(path):
    method = pd.read_csv(os.path.join(path, 'method.csv'))

    method['method'] = method.trace.str.split(';').str[0]
    method = method.groupby(['method', 'iter']).energy.sum()
    method = method.unstack()
    method /= method.sum()

    method = method.corr()

    plt.figure(figsize = (12, 9))
    ax = sns.heatmap(method, vmin = 0.75, vmax = 1, annot = True, fmt = ".2f", cmap = 'Reds', annot_kws = {'fontsize': 20})

    ax.collections[0].colorbar.set_label('correlation coefficient', fontsize = 20)
    ax.collections[0].colorbar.ax.tick_params(labelsize = 16)

    plt.xlabel('iteration', fontsize = 20)
    plt.ylabel('iteration', fontsize = 20)

    plt.xticks(fontsize = 24)
    plt.yticks(fontsize = 24)

    plt.savefig(os.path.join(path, '..', 'plots', 'auto-corr.pdf'), bbox_inches = 'tight')
    plt.close()

    return method
