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

def parse_timestamp(path):
    ts = np.sort([int(t) for t in json.load(open(path)).values()])
    return (np.max(ts) - np.min(ts)) / 100000000000

_RAPL_WRAPAROUND = 16384

def rapl_wrap_around(reading):
    if reading >= 0:
        return reading
    else:
        return max(reading + _RAPL_WRAPAROUND, 0)

def parse_energy(path):
    energy = pd.read_csv(path, delimiter = ';')

    energy.package = energy.groupby('socket').package.diff()
    energy.dram = energy.groupby('socket').dram.diff()

    energy.package = energy.package.map(rapl_wrap_around)
    energy.dram = energy.dram.map(rapl_wrap_around)
    energy = energy.fillna(0)

    return energy[['package', 'dram']].sum().sum()

def main():
    if not os.path.exists('plots'):
        os.mkdir('plots')
    root = os.path.join('..', 'chappie-data', 'fse2020')

    ref_dir = os.path.join(root, 'freq')
    data_dir = os.path.join(root, 'calmness')
    file_from = lambda k: os.path.join('raw', str(k))

    benchs = np.sort(os.listdir(ref_dir))
    # benchs = ['h2']
    benchs = tqdm(benchs)

    summary = []

    for bench in benchs:
        benchs.set_description(bench + " - ref")

        if not os.path.exists('plots/{}'.format(bench)):
            os.mkdir('plots/{}'.format(bench))

        if bench in ('fop', 'jme', 'kafka'):
            a = 20
            b = 100
        else:
            a = 2
            b = 10

        stats = []
        for rate in os.listdir(os.path.join(data_dir, bench)):
            # print(rate)
            # if rate != '1' and rate != '32' and rate != '512':
            #     continue
            benchs.set_description(bench + " - " + rate)

            e = [parse_energy(
                os.path.join(data_dir, bench, rate, file_from(str(k)), 'energy.csv')
            ) for k in range(a, b)]

            t = [parse_timestamp(
                os.path.join(data_dir, bench, rate, file_from(str(k)), 'time.json')
            ) for k in range(a, b)]

            d = {
                'e_m': np.mean(e),
                't_m': np.mean(t),
                'e_s': np.std(e),
                't_s': np.std(t),
            }

            d['p_m'] = d['e_m'] / d['t_m']
            d['p_s'] = np.sqrt((d['e_s'] / d['e_m'])**2 + (d['t_s'] / d['t_m'])**2)

            d['rate'] = int(rate)

            stats.append(pd.Series(d))
            benchs.set_description(bench)

        df = pd.concat(stats, axis = 1).T.set_index('rate').sort_index()[['e_m', 'e_s', 't_m', 't_s', 'p_m', 'p_s']] * 100
        df.index = df.index.astype(int)
        df.columns = pd.MultiIndex.from_tuples(product(('e', 't', 'p'), ('m', 's')))
        summary.append(df['t'].assign(benchmark = bench))

        continue

        # for col, color, label in zip(['e', 't', 'p'], [u'#2ca02c', u'#d62728', u'#1f77b4'], ['Energy (J)', 'Runtime (s)', 'Power (W)']):
        #     ax = df[col].plot.line(
        #         y = 'm',
        #         yerr = 's',
        #         lw = 2,
        #         capsize = 10,
        #         capthick = 1,
        #         color = color,
        #         legend = False,
        #         figsize = (25, 10),
        #     )
        #
        #     plt.title(bench, fontsize = 32)
        #
        #     ax.set_xlim(-0.5, 9.5)
        #
        #     plt.xlabel('Sampling Rate (ms)', fontsize = 20)
        #     plt.ylabel(label, fontsize = 20)
        #
        #     plt.xticks(fontsize = 20, rotation = 30)
        #     plt.yticks(fontsize = 24)
        #
        #     plt.savefig('plots/{}/{}.pdf'.format(bench, col), bbox_inches = 'tight')
        #     plt.close()

        # continue

        df = pd.concat(stats, axis = 1).T.set_index('rate').sort_index()[['e_m', 't_m', 'p_m', 'e_s', 't_s', 'p_s']] * 100
        # print(df)

        power = []
        for rate in [1, 2, 4, 8, 16, 32, 64, 128, 256, 512]:
            ts = [parse_timestamp(os.path.join(root, 'stability', bench, str(rate), str(n), 'raw', str(k), 'time.json')) / 1000000000 for n, k in product(range(0, 1), range(a, b))]

            df = pd.concat([parse_energy(os.path.join(
                root,
                'stability',
                bench,
                str(rate),
                str(n),
                'raw',
                str(k),
                'energy.csv'
            )) for n, k in product(range(0, 1), range(a, b))]).reset_index().groupby('socket').agg(('mean', 'std')).sum()
            df['runtime', 'mean'] = np.mean(ts)
            df['runtime', 'std'] = np.std(ts)
            df['power', 'mean'] = df['energy', 'mean'] / df['runtime', 'mean']
            df['power', 'std'] = np.sqrt((df['energy', 'std'] / df['energy', 'mean'])**2 + (df['runtime', 'std'] / df['runtime', 'mean'])**2)
            df['case'] = rate
            power.append(df)

            benchs.set_description(bench)

        df = pd.concat(power, axis = 1).T.set_index('case').sort_index()
        df.index = df.index.astype(int)
        df.index = df.index.astype(str)

        if not os.path.exists('plots/{}'.format(bench)):
            os.mkdir('plots/{}'.format(bench))

        for col, color, label in zip(['energy', 'runtime', 'power'], [u'#2ca02c', u'#d62728', u'#1f77b4'], ['Energy (J)', 'Runtime (s)', 'Power (W)']):
            ax = df[col].plot.line(
                y = 'mean',
                yerr = 'std',
                lw = 2,
                capsize = 10,
                capthick = 1,
                color = color,
                legend = False,
                figsize = (25, 10),
            )

            plt.title(bench, fontsize = 32)

            ax.set_xlim(-0.5, 9.5)

            plt.xlabel('Sampling Rate (ms)', fontsize = 20)
            plt.ylabel(label, fontsize = 20)

            plt.xticks(fontsize = 20, rotation = 30)
            plt.yticks(fontsize = 24)

            plt.savefig('plots/{}/{}.pdf'.format(bench, col), bbox_inches = 'tight')
            plt.close()

        df.index = df.index.astype(int)
        summary.append(df.reset_index().assign(bench = bench).set_index(['bench', 'case']))

    summary = pd.concat(summary).reset_index().set_index(['rate', 'benchmark'])['m'].unstack()
    # summary = summary.unstack(0)
    print(summary)

    summary.index = summary.index.astype(str)

    ax = summary.plot.line(
        style = ['o-', 's-', 'D-', 'h-', 'v-', 'P-', '^-', 'H-', '<-', '*-', '>-', 'X-', 'd-'],
        ms = 21,
        figsize = (25, 10)
    )

    ax.set_xlim(-0.5, 9.5)

    plt.legend(loc = 'upper right', fontsize = 20)

    plt.xlabel('Sampling Rate (ms)', fontsize = 36)
    plt.ylabel('Runtime (ms)', fontsize = 36)

    plt.xticks(fontsize = 40, rotation = 30)
    plt.yticks(fontsize = 40)

    plt.savefig('plots/runtime.pdf', bbox_inches = 'tight')
    plt.close()

    return

    for col, label in zip(['energy', 'runtime', 'power'], ['Energy (J)', 'Runtime (s)', 'Power (W)']):
        ax = summary[col].plot.line(
            y = 'mean',
            style = ['o-', 's-', 'D-', 'h-', 'v-', 'P-', '^-', 'H-', '<-', '*-', '>-', 'X-', 'd-'],
            ms = 10,
            figsize = (25, 10)
        )

        ax.set_xlim(-0.5, 9.5)

        plt.legend(loc = 'upper right', fontsize = 20)

        plt.xlabel('Sampling Rate (ms)', fontsize = 20)
        plt.ylabel(label, fontsize = 20)

        plt.xticks(fontsize = 20, rotation = 30)
        plt.yticks(fontsize = 24)

        plt.savefig('plots/{}.pdf'.format(col), bbox_inches = 'tight')
        plt.close()

if __name__ == '__main__':
    main()
