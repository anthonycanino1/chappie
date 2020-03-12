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

def parse_timestamp(path):
    ts = np.sort([int(t) for t in json.load(open(path)).values()])
    return (np.max(ts) - np.min(ts)) / 1000000000

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


    df = pd.read_csv('plots/temporal-correlation.csv', index_col = 'rate')
    print(df)
    df.index = df.index.astype(str)
    df[df < 0.7] = np.nan

    ax = df.plot.line(
        style = ['o-', 's-', 'D-', 'h-', 'v-', 'P-', '^-', 'H-', '<-', '*-', '>-', 'X-', 'd-'],
        ms = 21,
        figsize = (25, 10)
    )

    ax.set_xlim(-0.5, 9.5)

    plt.legend(loc = 'upper right', fontsize = 20)

    plt.xlabel('Sampling Rate (ms)', fontsize = 36)
    plt.ylabel('Temporal Correspondence', fontsize = 36)

    plt.xticks(fontsize = 40, rotation = 30)
    plt.yticks(fontsize = 40)

    plt.savefig('plots/temporal-correlation.pdf', bbox_inches = 'tight')
    plt.close()

    df = pd.read_csv('plots/spatial-correlation.csv', index_col = 'rate')
    df.index = df.index.astype(str)
    df[df < 0.7] = np.nan

    ax = df.plot.line(
        style = ['o-', 's-', 'D-', 'h-', 'v-', 'P-', '^-', 'H-', '<-', '*-', '>-', 'X-', 'd-'],
        ms = 21,
        figsize = (25, 10)
    )

    ax.set_xlim(-0.5, 9.5)

    plt.legend(loc = 'upper right', fontsize = 20)

    plt.xlabel('Sampling Rate (ms)', fontsize = 32)
    plt.ylabel('Spatial Correspondence', fontsize = 32)

    plt.xticks(fontsize = 36, rotation = 30)
    plt.yticks(fontsize = 36)

    plt.savefig('plots/spatial-correlation.pdf', bbox_inches = 'tight')
    plt.close()

    return

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

        # bins = np.linspace(1, 3, 101)
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

        # df.freq /= 10000
        # df.freq = df.freq.astype(int)
        # end = df.epoch.max()
        # df_len = len(df)
        #
        # size = bin_count(df.freq, end * 8)
        # freq_bins = np.linspace(120, 310, size + 1)
        # df['freq'] = pd.cut(df.freq, bins = freq_bins)
        # df = df.groupby(['cpu', 'freq']).epoch.count().to_frame().reset_index()
        #
        # size = bin_count(df.epoch, 40)
        # epoch_bins = np.linspace(0, end * 8, size + 1)
        # df.epoch = pd.cut(df.epoch, bins = epoch_bins)
        # df = df.groupby(['freq', 'epoch']).count()
        #
        # idx = pd.MultiIndex.from_product((freq_bins, epoch_bins))
        # df = df.reindex(idx).fillna(0).astype(int).cpu
        #
        # ref_ = df

        dists = []
        for rate in ['1', '32', '512']: # os.listdir(os.path.join(data_dir, bench)):
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

            freq_bins = np.linspace(df2.freq.min() - 1, df2.freq.max() + 1, f_size + 1)
            # freq_bins = [100, 200, 300, 400] # np.linspace(df2.freq.min() - 1, df2.freq.max() + 1, f_size + 1)
            if f_size > 1:
                df['freq'] = pd.cut(df.freq, bins = freq_bins)
                df = df.groupby(['epoch', 'freq', 'iter']).cpu.count().groupby(['epoch', 'freq']).mean()

                ref['freq'] = pd.cut(ref.freq, bins = freq_bins)
                ref = ref.groupby(['epoch', 'freq', 'iter']).cpu.count().groupby(['epoch', 'freq']).mean()
                # ref = ref / ref.groupby('epoch').sum()
            elif f_size == 1:
                df = df.groupby(['epoch']).cpu.count()

                ref = ref.groupby(['epoch']).cpu.count()

            df2 = pd.concat([df, ref])
            c_size = bin_count(df2, len(df2))

            # cpu_bins = np.linspace(1, 40, 40)
            df = df.round().fillna(0).astype(int)


            # cpu_bins = np.linspace(0, 100, 41)
            # cpu_bins = [-1, 20, 41]
            if c_size > 1:
                df = df.to_frame().reset_index()
                # df['cpu'] = pd.cut(df.cpu, bins = cpu_bins, include_lowest = True)
                df = df.groupby(['freq', 'cpu']).count().epoch.fillna(0).astype(int)

                ref = ref.to_frame().reset_index()
                # ref['cpu'] = pd.cut(ref.cpu, bins = cpu_bins, include_lowest = True)
                ref = ref.groupby(['freq', 'cpu']).count().epoch.fillna(0).astype(int)

            # fjdsakfldas

            # print(df.unstack())
            # print(ref.unstack())
            # print(df.to_frame().reset_index().groupby(['freq', 'cpu']).count().dropna())
            # fjdlsajfkdlsa

            # df.freq /= 10000
            # df.freq = df.freq.astype(int)
            # df.epoch //= df.epoch.min()
            #
            # df['freq'] = pd.cut(df.freq, bins = freq_bins)
            # df = df.groupby(['cpu', 'freq']).epoch.count().to_frame().reset_index()
            #
            # df.epoch = pd.cut(df.epoch, bins = epoch_bins)
            # df = df.groupby(['freq', 'epoch']).count()
            #
            # df = df.reindex(idx).fillna(0).astype(int).cpu

            dists.append((int(rate), df.corr(ref)))

            df = df.to_frame().reset_index()
            df.freq = pd.cut(df.freq.map(lambda x: (x.left + x.right) / 2), bins = [100, 150, 200, 250, 350])
            # df.cpu = pd.cut(df.freq.map(lambda x: (x.left + x.right) / 2), bins = [100, 150, 200, 250, 350])
            df = df.groupby(['cpu', 'freq']).sum()
            df = df / df.sum()
            df = df.unstack()
            df = df.reindex(pd.RangeIndex(1 , 41)).fillna(0).stack()
            # df = df / df.sum()

            # print(df.unstack().interpolate(limit_direction = 'both'))

            # ax = df.unstack().plot.bar(
            # # _, ax = plt.subplots()
            # # plt.figure(figsize = (12, 9))
            # # sns.heatmap(df.unstack().T)
            # # colors = [u'#1f77b4', u'#ff7f0e', u'#2ca02c', u'#d62728']
            # # ax = (100 * df).groupby('freq').plot.bar(
            #         # width = 1.0,
            #         # color = colors,
            #         # ax = ax,
            #         figsize = (12, 8),
            #         subplots = True
            #     )
            # for (_, s), c in zip(df.groupby('freq'), colors):
            #     (100 * s).plot.bar(
            #     # ax = df.unstack().plot.bar(
            #         stacked = True,
            #         width = 1.0,
            #         alpha = 0.5,
            #         color = c,
            #         ax = ax,
            #     )

            ax = (100 * df).unstack().plot.bar(
                stacked = True,
                width = 0.95,
                figsize = (12, 9)
            )

            handles, labels = ax.get_legend_handles_labels()
            ax.legend(handles[::-1], ['[1.0, 1.5)', '[1.5, 2.0)', '[2.0, 2.5)', '[2.5, 3.0)'][::-1], loc = 'upper right', fontsize = 20, title = 'frequency (GHz)')

            plt.title('')

            plt.xlabel('Cores Operating At Frequency', fontsize = 32)
            plt.ylabel('Chance of Occurance', fontsize = 32)

            import matplotlib.ticker as mtick

            ax.xaxis.set_major_locator(plt.MultipleLocator(4))
            ax.xaxis.set_minor_locator(plt.MultipleLocator(1))
            plt.xticks(range(3, 40, 4), range(4, 41, 4), rotation = 45, fontsize = 36)

            ax.yaxis.set_major_formatter(mtick.PercentFormatter())
            plt.ylim(0, 30)
            plt.yticks(fontsize = 36)

            plt.minorticks_on()

            plt.savefig('plots/{}/freq-spatial-{}ms.pdf'.format(bench, rate), bbox_inches = 'tight')
            plt.close()

        ref = ref.to_frame().reset_index()
        ref.freq = pd.cut(ref.freq.map(lambda x: (x.left + x.right) / 2), bins = [100, 150, 200, 250, 350])
        # ref.cpu = pd.cut(ref.cpu.map(lambda x: (x.left + x.right) / 2), bins = [10 * i for i in range(11)])
        ref = ref.groupby(['cpu', 'freq']).sum()
        ref = ref / ref.groupby('cpu').sum()

        ax = ref.unstack().plot.bar(
            # stacked = True,
            width = 1.0,
            figsize = (12, 9)
        )

        handles, labels = ax.get_legend_handles_labels()
        ax.legend(handles[::-1], ['[1.0, 1.5)', '[1.5, 2.0)', '[2.0, 2.5)', '[2.5, 3.0)'][::-1], loc = 'lower right', fontsize = 20, title = 'frequency (GHz)')

        plt.title(bench, fontsize = 32)

        plt.xlabel('CPU Count', fontsize = 20)
        plt.ylabel('')

        plt.xticks(range(0, 41, 4), rotation = 45, fontsize = 28)
        plt.yticks([])

        plt.savefig('plots/{}/freq-spatial-reference.pdf'.format(bench, rate), bbox_inches = 'tight')
        plt.close()

        df = pd.DataFrame(data = dists, columns = ['rate', 'correlation']).set_index('rate').sort_index()

        summary.append(df.reset_index().assign(bench = bench))
    df = pd.concat(summary).set_index(['rate', 'bench']).correlation
    df = df.unstack()
    df.to_csv('plots/spatial-correlation.csv')
    # print(df)

if __name__ == '__main__':
    main()
