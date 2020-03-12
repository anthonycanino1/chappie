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

rates = {
    'avrora': 128,
    'batik': 8,
    'biojava': 8,
    'eclipse': 16,
    'fop10': 4,
    'graphchi': 16,
    'h2': 32,
    'jme10': 128,
    'jython': 8,
    'pmd': 16,
    'sunflow': 16,
    'tomcat': 16,
    'xalan': 16
}

if __name__ == '__main__':
    root = '../chappie-data/fse2020'

    stats = []

    benchs = tqdm(np.sort(os.listdir(os.path.join(root, 'baseline'))))
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

        batches = df.batch.max() + 1
        df['method'] = df.trace.str.split(';').str[0]
        df = df.groupby(['method', 'batch']).energy.sum()
        # df.unstack().corr().to_csv('plots/{}-corr.csv'.format(bench))
        df = df.reset_index()

        df = pd.concat([
            df[df.batch < df.batch.max()].assign(g = 0),
            df.assign(g = 1)
        ])
        df = df.groupby(['method', 'g']).energy.sum()
        df = df.unstack()
        df = df / df.sum()

        corr = df.corr().loc[0, 1]
        corr_err = np.sqrt((1 - corr ** 2) / (len(df) - 2))

        rms = (df[0] - df[1]) ** 2 / len(df)
        rms = np.sqrt(rms.sum())

        stats.append(pd.Series(
            index = ['bench', 'batches', 'pcc', 'pcc_err', 'rmse'],
            data = [bench, batches, corr, corr_err, rms]
        ))

    stats = pd.concat(stats, axis = 1).T.set_index('bench').astype(float).round(4)
    stats.batches = stats.batches.astype(int)
    stats['rate'] = stats.index.map(rates)
    # [128, 8, 8, 16, 4, 16, 32, 128, 8, 16, 16, 16, 16]
    stats = stats[['rate', 'batches', 'pcc', 'pcc_err', 'rmse']]
    # stats.to_csv('plots/convergence.csv')
    print(stats)

    stats = stats.reset_index().transform(lambda x: x.map('{} & '.format)).sum(axis = 1).str[:-1].map(lambda x: x[:-1] + ' \\\\\n')
    table = stats.values

    with open('plots/convergence-table.tex', 'w') as f:
        [f.write(row) for row in table]
