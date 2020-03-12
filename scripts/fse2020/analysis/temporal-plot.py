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

from scipy.spatial.distance import *
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

def parse_timestamp(path):
    ts = np.sort([int(t) for t in json.load(open(path)).values()])
    return (np.max(ts) - np.min(ts)) / 1000000000

def bin_count(s, n):
    iqr = s.quantile(0.75) - s.quantile(0.25)
    if iqr > 0:
        d = s.max() - s.min()
        size = int(d * np.cbrt(n) / iqr / 2)
    else:
        size = 21

    if size > 21:
        size = 21

    return size

def main():
    if not os.path.exists('plots'):
        os.mkdir('plots')
    root = os.path.join('..', 'chappie-data', 'fse2020')

    ref_dir = os.path.join(root, 'freq')
    data_dir = os.path.join(root, 'calmness')
    freq_file = lambda k: os.path.join('raw', str(k), 'freqs.csv')
    file_from = lambda k: os.path.join('raw', str(k))

    benchs = np.sort(os.listdir(ref_dir))
    benchs = ['h2']
    benchs = tqdm(benchs)

    summary = []

    for bench in benchs:
        benchs.set_description(bench + " - ref")

        if not os.path.exists('plots/{}'.format(bench)):
            os.mkdir('plots/{}'.format(bench))

        if bench == 'fop10' or bench == 'jme10':
            continue

        if bench in ('fop10', 'jme10', 'kafka10'):
            a = 20
            b = 100
        else:
            a = 2
            b = 10

        df = pd.concat([pd.read_csv(
            os.path.join(ref_dir, bench, freq_file(k)),
            delimiter = ';'
        ).assign(iter = k) for k in range(a, b)])
        df.freq /= 10000
        df.freq = df.freq.astype(int)

        df.epoch = df.epoch * df.epoch.max() / df.groupby('iter').epoch.max().mean()
        df.epoch = df.epoch.round(0).astype(int)

        ref_ = df

        t_ref = [parse_timestamp(
            os.path.join(ref_dir, bench, file_from(str(k)), 'time.json')
        ) for k in range(a, b)]

        dists = []
        for rate in ['1']: # os.listdir(os.path.join(data_dir, bench)):
            benchs.set_description(bench + " - " + rate)

            t = [parse_timestamp(
                os.path.join(data_dir, bench, rate, file_from(str(k)), 'time.json')
            ) for k in range(a, b)]

            # if not (np.mean(t) <= 1.05 * np.mean(t_ref) and np.mean(t) >= 0.95 * np.mean(t_ref)) and not (np.mean(t) <= 2 * np.std(t_ref) + np.mean(t_ref) and np.mean(t) >= np.mean(t_ref) - 2 * np.std(t_ref)):
            #     dists.append((int(rate), np.nan))
            #     continue

            ref = ref_.copy(deep = True)

            df = pd.concat([pd.read_csv(
                os.path.join(data_dir, bench, rate, freq_file(k)),
                delimiter = ';'
            ).assign(iter = k) for k in range(a, b)])
            df.epoch //= df.epoch.min()
            df.freq /= 10000
            df.freq = df.freq.astype(int)

            df.epoch = df.epoch * ref.epoch.max() / df.groupby('iter').epoch.max().mean()
            df.epoch = df.epoch.round(0).astype(int)

            df2 = pd.concat([df, ref])
            f_size = bin_count(df2.freq, len(df2)) # 640 * df2.epoch.max())
            print(f_size)

            # print(f_size)

            freq_bins = np.linspace(df2.freq.min() - 1, df2.freq.max() + 1, f_size + 1)
            # epoch_bins = np.linspace(df2.epoch.min(), df2.epoch.max(), df2.epoch.max() // int(np.sqrt(df2.epoch.max()))
            if f_size > 1:
                df['freq'] = pd.cut(df.freq, bins = freq_bins)
                # df['epoch'] = pd.cut(df.epoch, bins = epoch_bins, include_lowest = True)
                df = df.groupby(['epoch', 'freq']).cpu.count()
                df = df / df.groupby('epoch').sum()

                ref['freq'] = pd.cut(ref.freq, bins = freq_bins)
                # ref['epoch'] = pd.cut(ref.epoch, bins = epoch_bins, include_lowest = True)
                ref = ref.groupby(['epoch', 'freq']).cpu.count() # .groupby(['epoch', 'freq']).mean()
                ref = ref / ref.groupby('epoch').sum()
            elif f_size == 1:
                # df['epoch'] = pd.cut(df.epoch, bins = epoch_bins, include_lowest = True)
                df = df.groupby(['epoch']).cpu.count()

                # ref['epoch'] = pd.cut(ref.epoch, bins = epoch_bins, include_lowest = True)
                ref = ref.groupby(['epoch']).cpu.count()

            # print(df.unstack())
            # print(ref.unstack())

            # df = pd.concat([df, ref], axis = 1).fillna(0).reset_index()
            # df = df[df.epoch == 228]
            # print(df)
            # print(pd.concat([df, ref], axis = 1).fillna(0).unstack())
            # print(pd.concat([df, ref], axis = 1).fillna(0).corr())

            # fdjkfdajskl

            # dists.append((int(rate), df.corr(ref)))
            # df = df.unstack().tail(120).stack()
            # ref = ref.unstack().tail(120).stack()
            dists.append((int(rate), df.corr(ref)))
            # continue

            df = df / df.groupby(['epoch']).sum()
            df = df.to_frame().reset_index()
            # df.freq = pd.cut(df.freq.map(lambda x: (x.left + x.right) / 2), bins = [100, 110, 120, 130, 140, 150, 160, 170, 180, 190, 200, 210, 220, 230, 240, 250, 260, 270, 280, 290, 300, 310, 320, 330, 340, 350])
            df.freq = pd.cut(df.freq.map(lambda x: (x.left + x.right) / 2), bins = [100, 125, 150, 175, 200, 225, 250, 275, 350])
            df = df.groupby(['epoch', 'freq']).sum()

            # print(df.unstack())

            ax = df.unstack().plot.bar(
                stacked = True,
                width = 0.8,
                figsize = (10.5, 9)
            )

            handles, labels = ax.get_legend_handles_labels()
            ax.legend(handles[::-1], ['[1.0, 1.25)', '[1.25, 1.5)', '[1.5, 1.75)', '[1.75, 2.0)', '[2.0, 2.25)', '[2.25, 2.5)', '[2.5, 2.75)', '[2.75, 3.0)'][::-1], loc = 'lower right', fontsize = 20, title = 'frequency (GHz)')

            ax.spines['right'].set_visible(False)
            ax.spines['top'].set_visible(False)
            ax.spines['left'].set_visible(False)

            plt.xlabel('Epoch', fontsize = 32)
            plt.ylabel('')

            plt.xticks([])
            plt.yticks([])

            plt.savefig('plots/{}/freq-temporal-{}ms.pdf'.format(bench, rate), bbox_inches = 'tight')
            plt.close()

        continue

        ref = ref.to_frame().reset_index()
        # ref.freq = pd.cut(ref.freq.map(lambda x: (x.left + x.right) / 2), bins = [100, 110, 120, 130, 140, 150, 160, 170, 180, 190, 200, 210, 220, 230, 240, 250, 260, 270, 280, 290, 300, 310, 320, 330, 340, 350])
        ref.freq = pd.cut(ref.freq.map(lambda x: (x.left + x.right) / 2), bins = [100, 150, 200, 250, 300])
        ref = ref.groupby(['epoch', 'freq']).sum()
        ref = ref / ref.groupby('epoch').sum()

        ax = ref.unstack().plot.bar(
            stacked = True,
            width = 1.0,
            figsize = (12, 9)
        )

        handles, labels = ax.get_legend_handles_labels()
        ax.legend(handles[::-1], ['[1.0, 1.25)', '[1.25, 1.5)', '[1.5, 1.75)', '[1.75, 2.0)', '[2.0, 2.25)', '[2.25, 2.5)', '[2.5, 2.75)', '[2.75, 3.0)'][::-1], loc = 'lower right', fontsize = 20, title = 'frequency (GHz)')

        # plt.title(bench, fontsize = 32)

        plt.xlabel('Epoch', fontsize = 32)
        # plt.ylabel('')

        plt.xticks([])
        plt.yticks([])

        plt.savefig('plots/{}/freq-temporal-reference.pdf'.format(bench, rate), bbox_inches = 'tight')
        plt.close()

        df = pd.DataFrame(data = dists, columns = ['rate', 'correlation']).set_index('rate').sort_index()
        print(df)

        summary.append(df.reset_index().assign(bench = bench))
    df = pd.concat(summary).set_index(['rate', 'bench']).correlation
    df = df.unstack()
    # df.to_csv('plots/temporal-correlation.csv')
    # print(df)

if __name__ == '__main__':
    main()
