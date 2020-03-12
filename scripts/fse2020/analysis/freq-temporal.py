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

def main():
    if not os.path.exists('plots'):
        os.mkdir('plots')

    root = '../chappie-data/fse2020'

    # benchs = np.sort(os.listdir('../chappie-data/fse2020/nop'))
    benchs = ['h2']
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
        ), delimiter = ';') for k in range(a, b)]).groupby(['cpu', 'epoch']).freq.mean() / 1000000
        df = df.unstack()
        df.index += 1
        df = df.T
        df = df.reset_index()
        df['epoch'] = pd.cut(df.epoch, bins = 50)
        df = df.groupby('epoch').mean().T
        print(df)
        # df.to_csv('nop.csv')

        plt.figure(figsize = (12, 9))
        ax = sns.heatmap(df, vmin = 1, vmax = 3, cmap = 'Reds')

        ax.collections[0].colorbar.set_label('cpu frequency (GHz)', fontsize = 20)
        ax.collections[0].colorbar.ax.tick_params(labelsize = 16)

        plt.xlabel('Epoch', fontsize = 20)
        plt.ylabel('CPU', fontsize = 20)

        plt.xticks([], fontsize = 16)
        plt.yticks(fontsize = 12)

        plt.savefig('plots/{}/freq-temporal-nop.pdf'.format(bench), bbox_inches = 'tight')
        plt.close()

        for rate in [1, rates[bench]]:
            df = pd.concat([pd.read_csv(os.path.join(
                root,
                'stability',
                bench,
                str(rate),
                str(n),
                'raw',
                str(k),
                'cpu.csv'
            ), delimiter = ';', usecols = ['epoch', 'cpu', 'freq']) for n, k in product(range(0, 1), range(a, b))]).groupby(['cpu', 'epoch']).freq.mean() / 1000000
            df = df.unstack()
            df.index += 1
            df = df.T
            df = df.reset_index()
            df['epoch'] = pd.cut(df.epoch, bins = 50)
            df = df.groupby('epoch').mean().T

            # df.to_csv('{}.csv'.format(rate))

            plt.figure(figsize = (12, 9))
            ax = sns.heatmap(df, vmin = 1, vmax = 3, cmap = 'Reds')

            ax.collections[0].colorbar.set_label('cpu frequency (GHz)', fontsize = 20)
            ax.collections[0].colorbar.ax.tick_params(labelsize = 16)

            plt.xlabel('Epoch', fontsize = 20)
            plt.ylabel('CPU', fontsize = 20)

            plt.xticks([], fontsize = 16)
            plt.yticks(fontsize = 12)

            plt.savefig('plots/{}/freq-temporal-{}ms.pdf'.format(bench, rate), bbox_inches = 'tight')
            plt.close()

            benchs.set_description(bench)

if __name__ == '__main__':
    main()
