#!/usr/bin/python3

import json
import os

from itertools import product

import matplotlib
matplotlib.use('Agg')

import matplotlib.patheffects as path_effects
import matplotlib.pyplot as plt
import numpy as np
import pandas as pd

from tqdm import tqdm

def main():
    if not os.path.exists('plots'):
        os.mkdir('plots')

    root = '../chappie-data/fse2020'

    benchs = np.sort(os.listdir('../chappie-data/fse2020/nop'))
    # benchs = ['batik', 'biojava', 'jython']
    benchs = tqdm(benchs)

    summary = []

    for bench in benchs:
        if bench == 'kafka10':
            continue
        if bench in ('fop', 'jme', 'kafka'):
            bench_ = bench + '10'
        else:
            bench_ = bench
        if not os.path.exists('plots/{}'.format(bench_)):
            os.mkdir('plots/{}'.format(bench_))

        benchs.set_description(bench)

        if bench in ('fop10', 'jme10', 'kafka10'):
            a = 20
            b = 100
        else:
            a = 2
            b = 10

        freqs = []

        df = pd.concat([pd.read_csv(os.path.join(
            root,
            'nop',
            bench,
            'raw',
            str(k),
            'freqs.csv'
        ), delimiter = ';') for k in range(a, b)]).freq.agg(('mean', 'std')) / 1000000
        df['case'] = 1000

        for rate in [1, 2, 4, 8, 16, 32, 64, 128, 256, 512]:
            df = pd.concat([pd.read_csv(os.path.join(
                root,
                'stability',
                bench,
                str(rate),
                str(n),
                'raw',
                str(k),
                'cpu.csv'
            ), delimiter = ';', usecols = ['freq']) for n, k in product(range(0, 1), range(a, b))]).freq.agg(('mean', 'std')) / 1000000
            df['case'] = rate
            freqs.append(df)

            benchs.set_description(bench)

        df = pd.concat(freqs, axis = 1).T.set_index('case').sort_index()
        df.index = df.index.astype(int)
        df.index = df.index.astype(str)

        if not os.path.exists('plots/{}'.format(bench)):
            os.mkdir('plots/{}'.format(bench))

        ax = df.plot.line(
            y = 'mean',
            yerr = 'std',
            lw = 2,
            capsize = 10,
            capthick = 1,
            color = u'#d62728',
            legend = False,
            figsize = (25, 10),
        )

        plt.title(bench, fontsize = 32)

        ax.set_xlim(-0.5, 9.5)

        plt.xlabel('Sampling Rate (ms)', fontsize = 20)
        plt.ylabel('Frequency (GHz)', fontsize = 20)

        plt.xticks(fontsize = 20, rotation = 30)
        plt.yticks(fontsize = 24)

        plt.savefig('plots/{}/freq.pdf'.format(bench), bbox_inches = 'tight')
        plt.close()

        df.index = df.index.astype(int)
        summary.append(df.reset_index().assign(bench = bench).set_index(['bench', 'case']))

    summary = pd.concat(summary)
    summary = summary.unstack(0)
    print(summary)

    summary.index = summary.index.astype(str)

    ax = summary.plot.line(
        y = 'mean',
        style = ['o-', 's-', 'D-', 'h-', 'v-', 'P-', '^-', 'H-', '<-', '*-', '>-', 'X-', 'd-'],
        ms = 10,
        figsize = (25, 10)
    )

    ax.set_xlim(-0.5, 9.5)

    plt.legend(loc = 'upper right', fontsize = 20)

    plt.xlabel('Sampling Rate (ms)', fontsize = 20)
    plt.ylabel('Frequency (GHz)', fontsize = 20)

    plt.xticks(fontsize = 20, rotation = 30)
    plt.yticks(fontsize = 24)

    plt.savefig('plots/freq.pdf', bbox_inches = 'tight')
    plt.close()

if __name__ == '__main__':
    main()
