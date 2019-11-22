import os

import matplotlib.pyplot as plt
import numpy as np
import pandas as pd

from tqdm import tqdm

def ranking(path):
    method = pd.read_csv(os.path.join(path, 'method.csv')).groupby('trace')[['energy', 'time']].sum().reset_index()

    method['method'] = method.trace.str.split(';').str[0]
    method = method.groupby('method')[['energy', 'time']].sum()
    method /= method.sum()
    method = method.reset_index()

    df = method.copy(deep = True)
    df.loc[:, 'package'] = df.method.str.split('.').str[:-2].str.join('.')
    df.loc[:, 'class'] = df.method.str.split('.').str[-2]
    df.loc[:, 'method'] = df.method.str.split('.').str[-2:].str.join('.')

    for col in ('method', 'class', 'package'):
        df_ = df.groupby(col).sum().sort_values('energy')
        df_.index = df_.index.str.replace('$', '\$')
        if col == 'method':
            y = ['time', 'energy']
            c = [u'#d62728', u'#2ca02c']
            # c = ['tab:red', 'tab:green']
            df.to_csv(os.path.join(path, '..', 'summary', 'method_ranking.csv'))

            rankings = df_
        else:
            y = 'energy'
            c = u'#2ca02c'
            # c = 'tab:green'

        ax = df_.tail(10).plot(
            kind = 'barh', y = y,
            width = 0.33, align = 'edge', color = c,
            figsize = (16, 9)
        )

        ax.spines['right'].set_visible(False)
        ax.spines['top'].set_visible(False)

        for rect, name in zip(ax.patches, df_.tail(10).index):
            height = rect.get_height()
            ax.text(
                df_.max().max() * 0.005, rect.get_y() + height + (0.20 if col == 'method' else 0.05),
                name,
                ha='left', va='bottom', fontsize = 20
            )

        if col == 'method':
            handles, labels = ax.get_legend_handles_labels()
            ax.legend(handles[::-1], labels[::-1], loc = 'lower right', fontsize = 24)
        else:
            ax.get_legend().remove()

        plt.xlim(0, (df_ * 100).apply(np.ceil).energy.max() / 100)

        plt.xlabel('Normalized Value', fontsize = 20)
        plt.ylabel(col.title(), fontsize = 20)

        plt.yticks([])
        plt.xticks(fontsize = 16)

        plt.savefig(os.path.join(path, '..', 'plots', '{}_ranking.pdf'.format(col)), bbox_inches = 'tight')
        plt.close()

    return rankings.sort_values('energy', ascending = False)
