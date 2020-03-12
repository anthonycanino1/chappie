#!/usr/bin/python3

import argparse
import os

from time import time

import matplotlib
matplotlib.use('Agg')

import matplotlib.pyplot as plt
import numpy as np
import pandas as pd
import seaborn as sns

from tqdm import tqdm

def ranking_plot(df):
    df = df.sort_values(df.columns[0])

    df.index = df.index.str.replace('$', '\$')
    y = df.columns[::-1]
    c = [u'#2ca02c', u'#d62728', u'#2ca02c'][:len(y)]

    ax = df.tail(10).plot.barh(
        y = y,
        width = 0.33,
        legend = False,
        align = 'edge',
        color = c,
        figsize = (16, 9)
    )

    ax.spines['right'].set_visible(False)
    ax.spines['top'].set_visible(False)

    for rect, name in zip(ax.patches, df.tail(10).index):
        height = rect.get_height()
        ax.text(
            df.max().max() * 0.005,
            (rect.get_y() + height + 0.20) if len(y) > 1 else (rect.get_y() + height + 0.05),
            name,
            ha='left', va='bottom', fontsize = 24
        )

    if len(y) > 1:
        handles, labels = ax.get_legend_handles_labels()
        ax.legend(handles[::-1], labels[::-1], loc = 'lower right', fontsize = 24)

    plt.xlim(0, ((5 * df.energy.max()).astype(int) + 1) / 5)

    plt.xlabel('Normalized Energy Consumption', fontsize = 28)
    plt.ylabel(df.index.name.title(), fontsize = 28)

    plt.yticks([])
    plt.xticks(fontsize = 32)

    return ax.get_figure()

if __name__ == '__main__':
    root = '../chappie-data/fse2020'

    stats = []

    benchs = tqdm(np.sort(os.listdir(os.path.join(root, 'baseline'))))
    benchs = tqdm(['batik'])
    for bench in benchs:
        benchs.set_description(bench)

        df = pd.concat([
            pd.read_csv(os.path.join(
                root,
                'baseline',
                bench,
                batch,
                'summary',
                'method.csv'
            )).assign(batch = int(batch)) for batch in os.listdir(os.path.join(root, 'baseline', bench))
        ])
        if len(df) == 0:
            continue

        df['method_'] = df.trace.str.split(';').str[0]

        ranking = df.copy(deep = True).groupby('method_')[['energy', 'time']].sum()
        ranking.energy = ranking.energy / ranking.energy.sum()
        ranking.time = ranking.time / ranking.time.sum()

        ranking['method'] = ranking.index.str.split('.').str[-2:].str.join('.')
        ranking_plot(ranking.set_index('method'))
        plt.savefig(os.path.join('plots', bench, 'method-ranking.pdf'.format(bench)), bbox_inches = 'tight')
        plt.close()

        ranking['method'] = ranking.index.str.split('.').str[-2:].str.join('.')
        ranking_plot(ranking.set_index('method')[['energy']])
        plt.savefig(os.path.join('plots', bench, 'energy-method-ranking.pdf'.format(bench)), bbox_inches = 'tight')
        plt.close()

        ranking['class'] = ranking.index.str.split('.').str[-2]
        ranking_plot(ranking.groupby('class')[['energy']].sum())
        plt.savefig(os.path.join('plots', bench, 'class-ranking.pdf'.format(bench)), bbox_inches = 'tight')
        plt.close()

        ranking['package'] = ranking.index.str.split('.').str[:-2].str.join('.')
        ranking_plot(ranking.groupby('package')[['energy']].sum())
        plt.savefig(os.path.join('plots', bench, 'package-ranking.pdf'.format(bench)), bbox_inches = 'tight')
        plt.close()

        context1 = df.trace.str.split(';').str[1].str.split('.').str[-2:].str.join('.')
        context2 = df.trace.str.split(';').str[2].str.split('.').str[-2:].str.join('.')
        context = df.copy(deep = True).assign(context = "[" + context1 + ", " + context2 + "]")

        top_methods = context.groupby('method_').energy.sum().sort_values(ascending = False).head(10).index.values

        context = context[context.method_.isin(top_methods)].groupby(['method_', 'context']).energy.sum().reset_index()
        context['method'] = context.method_.str.split('.').str[-2:].str.join('.')
        context = context.set_index(['method', 'context']).energy
        context /= context.groupby('method').sum()

        for i, (method, df) in enumerate(context.reset_index().groupby('method')):
            print(method)
            ax = df.plot.pie(
                x = 'context', y = 'energy',
                labels = None,
                wedgeprops = {'edgecolor': 'k', 'linewidth': 1},
                figsize = (24, 16)
            )

            plt.title(method, fontsize = 72)
            plt.xlabel('')
            plt.ylabel('')

            if len(df.context) < 15:
                ax.legend(df.context, loc = 'upper left', bbox_to_anchor = (0.925, 0.925), frameon = False, fontsize = 48)
            else:
                ax.legend(df.context, fontsize = 10, ncol = int(np.ceil(len(df.context) / 30)))

            plt.savefig(os.path.join('plots', bench, 'cfa2_{}.pdf'.format(i + 1)), bbox_inches = 'tight')
            plt.close()
