#!/usr/bin/python3

import json
import os
import os.path as op

from itertools import product

import matplotlib
matplotlib.use('Agg')

import matplotlib.pyplot as plt
import numpy as np
import pandas as pd

from tqdm import tqdm

rates = {
    'avrora': 128,
    'batik': 8,
    'biojava': 8,
    'eclipse': 16,
    'graphchi': 16,
    'h2': 32,
    'jython': 8,
    'pmd': 16,
    'sunflow': 16,
    'tomcat': 16,
    'xalan': 16
}

def main():
    if not op.exists('plots'):
        os.mkdir('plots')
    root = op.join('..', 'chappie-data', 'fse2020')

    ref_dir = op.join(root, 'freq')
    data_dir = op.join(root, 'profile')
    file_from = lambda k: op.join('raw', str(k))

    benchs = np.sort(os.listdir(ref_dir))
    benchs_ = tqdm(benchs)

    summary = []
    for bench in benchs_:
        benchs_.set_description(bench)

        if not op.exists('plots/{}'.format(bench)):
            os.mkdir('plots/{}'.format(bench))

        a = 2; b = 10

        df = pd.concat([pd.read_csv(
            op.join(data_dir, bench, str(n), 'summary', 'method.csv')
        ).assign(batch = n) for n in os.listdir(op.join(data_dir, bench))])

        batches = df.batch.max()

        df['method'] = df.trace.str.split(';').str[0]
        df = df.groupby(['method', 'batch']).energy.sum()
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

        summary.append(pd.Series(
            index = ['bench', 'batches', 'pcc', 'pcc_err', 'rmse'],
            data = [bench, batches, corr, corr_err, rms]
        ))

    df = pd.concat(summary, axis = 1).T.set_index('bench').astype(float).round(4)
    df.batches = df.batches.astype(int)
    df['rate'] = df.index.map(rates)
    df = df[['rate', 'batches', 'pcc', 'pcc_err', 'rmse']]
    print(df)

    df.index = df.index.map(lambda x: r'\texttt{' + x + '}')
    df = df.reset_index().transform(lambda x: x.map('{} & '.format)).sum(axis = 1).str[:-1].map(lambda x: x[:-1] + ' \\\\\n')
    table = df.values

    with open('plots/convergence-table.tex', 'w') as f:
        [f.write(row) for row in table]

if __name__ == '__main__':
    main()
