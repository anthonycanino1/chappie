#!/usr/bin/python3

import json
import os
import os.path as op

from itertools import product

import matplotlib
matplotlib.use('Agg')

import matplotlib.pyplot as plt
import matplotlib.ticker as mtick
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

def parse_timestamp(path):
    ts = np.sort([int(t) for t in json.load(open(path)).values()])
    return (np.max(ts) - np.min(ts)) / 1000000000

def within_bounded_error(t, ref):
    lower = np.mean(t) >= 0.95 * np.mean(ref) and np.mean(t) <= 1.05 * np.mean(ref)
    upper = np.mean(t) <= np.mean(ref) + 2 * np.std(ref) and np.mean(t) >= np.mean(ref) - 2 * np.std(ref)
    return lower or upper

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
    if not op.exists('plots'):
        os.mkdir('plots')
    root = op.join('..', 'chappie-data', 'fse2020')

    ref_dir = op.join(root, 'freq')
    data_dir = op.join(root, 'calmness')
    freq_file = lambda k: op.join('raw', str(k), 'freqs.csv')
    file_from = lambda k: op.join('raw', str(k))

    benchs = np.sort(os.listdir(ref_dir))
    benchs = tqdm(benchs)

    summary = []

    for bench in benchs:
        benchs.set_description(bench + " - ref")

        if not op.exists('plots/{}'.format(bench)):
            os.mkdir('plots/{}'.format(bench))

        a = 2; b = 10

        t_ref = [parse_timestamp(
            op.join(ref_dir, bench, file_from(str(k)), 'time.json')
        ) for k in range(a, b)]

        df = pd.concat([pd.read_csv(
            op.join(ref_dir, bench, freq_file(k)),
            delimiter = ';'
        ).assign(iter = k) for k in range(a, b)])
        df.freq /= 10000
        df.freq = df.freq.astype(int)

        df.epoch = df.epoch * df.epoch.max() / df.groupby('iter').epoch.max().mean()
        df.epoch = df.epoch.round(0).astype(int)

        ref_ = df

        dists = []
        for rate in os.listdir(op.join(data_dir, bench)):
            benchs.set_description(bench + ' - ' + str(rate))

            t = [parse_timestamp(
                op.join(data_dir, bench, str(rate), file_from(k), 'time.json')
            ) for k in range(a, b)]

            if not within_bounded_error(t, t_ref):
                dists.append((int(rate), np.nan))
                continue

            ref = ref_.copy(deep = True)

            df = pd.concat([pd.read_csv(
                op.join(data_dir, bench, str(rate), freq_file(k)),
                delimiter = ';'
            ).assign(iter = k) for k in range(a, b)])
            df.freq /= 10000
            df.freq = df.freq.astype(int)

            df.epoch //= df.epoch.min()
            df.epoch = df.epoch * ref.epoch.max() / df.groupby('iter').epoch.max().mean()
            df.epoch = df.epoch.round(0).astype(int)

            merged_data = pd.concat([df, ref])
            f_size = bin_count(merged_data.freq, len(merged_data))

            freq_bins = np.linspace(merged_data.freq.min() - 1, merged_data.freq.max() + 1, f_size + 1)
            if f_size > 1:
                df['freq'] = pd.cut(df.freq, bins = freq_bins)
                df = df.groupby(['epoch', 'freq']).cpu.count()
                df = df / df.groupby('epoch').sum()
                # df = df.groupby(['epoch', 'freq', 'iter']).cpu.count().groupby(['epoch', 'freq']).mean()

                ref['freq'] = pd.cut(ref.freq, bins = freq_bins)
                ref = ref.groupby(['epoch', 'freq']).cpu.count()
                ref = ref / ref.groupby('epoch').sum()
                # ref = ref.groupby(['epoch', 'freq', 'iter']).cpu.count().groupby(['epoch', 'freq']).mean()
            elif f_size == 1:
                df = df.groupby(['epoch']).cpu.count()
                ref = ref.groupby(['epoch']).cpu.count()

            merged_data = pd.concat([df, ref])
            c_size = bin_count(merged_data, len(df))

            cpu_bins = np.linspace(0, merged_data.max() + 1, c_size + 1)
            if c_size > 1:
                df = df.to_frame().reset_index()
                df['cpu'] = pd.cut(df.cpu, bins = cpu_bins, include_lowest = True)
                df = df.groupby(['freq', 'cpu']).count().epoch.fillna(0).astype(int)
                # df = df / df.groupby('freq').sum()

                ref = ref.to_frame().reset_index()
                ref['cpu'] = pd.cut(ref.cpu, bins = cpu_bins, include_lowest = True)
                ref = ref.groupby(['freq', 'cpu']).count().epoch.fillna(0).astype(int)
                # ref = ref / ref.groupby('freq').sum()
            elif c_size == 1:
                df = df.groupby(['freq']).cpu.count()
                ref = ref.groupby(['freq']).cpu.count()

            dists.append((int(rate), df.corr(ref)))

            df = df.to_frame().reset_index()
            df.freq = pd.cut(df.freq.map(lambda x: (x.left + x.right) / 2), bins = list(range(100, 351, 50)))
            df = df.groupby(['cpu', 'freq']).sum()
            df = df / df.sum()

            ax = (100 * df).unstack().plot.bar(
                stacked = True,
                width = 0.95,
                figsize = (12, 9)
            )

            handles, labels = ax.get_legend_handles_labels()
            ax.legend(handles[::-1], ['[1.0, 1.5)', '[1.5, 2.0)', '[2.0, 2.5)', '[2.5, 3.0)', '[3.0, 3.5)'][::-1], loc = 'lower right', fontsize = 20, title = 'frequency (GHz)')

            ax.spines['right'].set_visible(False)
            ax.spines['top'].set_visible(False)

            plt.xlabel('Cores Operating At Frequency', fontsize = 32)
            plt.ylabel('Chance of Occurance', fontsize = 32)

            ax.xaxis.set_major_locator(plt.MultipleLocator(4))
            ax.xaxis.set_minor_locator(plt.MultipleLocator(1))
            plt.xticks(range(0, 41, 4), range(0, 41, 4), rotation = 45, fontsize = 36)

            plt.ylim(0, 40)
            ax.yaxis.set_major_formatter(mtick.PercentFormatter())
            plt.yticks(fontsize = 36)

            plt.minorticks_on()

            plt.savefig('plots/{}/freq-spatial-{}ms.pdf'.format(bench, rate), bbox_inches = 'tight')
            plt.close()

        ref = ref.to_frame().reset_index()
        ref.freq = pd.cut(ref.freq.map(lambda x: (x.left + x.right) / 2), bins = list(range(100, 351, 50)))
        ref = ref.groupby(['cpu', 'freq']).sum()
        ref = ref / ref.sum()

        ax = (100 * ref).unstack().plot.bar(
            stacked = True,
            width = 1.0,
            figsize = (12, 9)
        )

        handles, labels = ax.get_legend_handles_labels()
        ax.legend(handles[::-1], ['[1.0, 1.5)', '[1.5, 2.0)', '[2.0, 2.5)', '[2.5, 3.0)', '[3.0, 3.5)'][::-1], loc = 'lower right', fontsize = 20, title = 'frequency (GHz)')

        ax.spines['right'].set_visible(False)
        ax.spines['top'].set_visible(False)

        plt.xlabel('Cores Operating At Frequency', fontsize = 32)
        plt.ylabel('Chance of Occurance', fontsize = 32)

        ax.xaxis.set_major_locator(plt.MultipleLocator(4))
        ax.xaxis.set_minor_locator(plt.MultipleLocator(1))
        plt.xticks(range(0, 41, 4), range(0, 41, 4), rotation = 45, fontsize = 36)

        plt.ylim(0, 40)
        ax.yaxis.set_major_formatter(mtick.PercentFormatter())
        plt.yticks(fontsize = 36)

        plt.minorticks_on()

        plt.savefig('plots/{}/freq-spatial-reference.pdf'.format(bench, rate), bbox_inches = 'tight')
        plt.close()

        df = pd.DataFrame(data = dists, columns = ['rate', 'correlation']).set_index('rate').sort_index()
        summary.append(df.reset_index().assign(bench = bench))

    df = pd.concat(summary).set_index(['rate', 'bench']).correlation
    df = df.unstack()
    df.to_csv('plots/spatial-correlation.csv')
    print(df)

    df.index = df.index.astype(str)
    ax = df.plot.line(
        style = ['o-', 's-', 'D-', 'h-', 'v-', 'P-', '^-', 'H-', '<-', '*-', '>-', 'X-', 'd-'],
        ms = 21,
        figsize = (25, 10)
    )

    ax.set_xlim(-0.5, 9.5)

    plt.legend(loc = 'upper right', fontsize = 20)

    plt.xlabel('Sampling Rate (ms)', fontsize = 36)
    plt.ylabel('Spatial Correspondence', fontsize = 36)

    plt.xticks(fontsize = 40, rotation = 30)
    plt.yticks(fontsize = 40)

    plt.savefig(op.join('plots', 'spatial-correlation.pdf'), bbox_inches = 'tight')
    plt.close()

if __name__ == '__main__':
    main()
