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

def energy_plot(df):
    ax = df.plot.bar(y = 'mean', yerr = 'std', stacked = True, edgecolor = 'black', width = 0.55, figsize = (16, 9), error_kw = dict(lw = 2, capsize = 10, capthick = 1))

    handles, labels = ax.get_legend_handles_labels()
    ax.legend(handles[::-1], ['dram 2', 'cpu 2', 'dram 1', 'cpu 1'], loc = 'upper left', fontsize = 20)

    plt.xlabel('Benchmark', fontsize = 20)
    plt.ylabel('Energy (J)', fontsize = 20)

    plt.xticks(fontsize = 20, rotation = 30)
    plt.yticks(fontsize = 24)

def parse_timestamp(path):
    ts = np.sort([int(t) for t in json.load(open(path)).values()])
    if 'fse2020/nop' in path:
        ts = ts[1:]
    return np.max(ts) - np.min(ts)

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

    energy = energy.groupby('socket')[['package', 'dram']].sum()

    energy['energy'] = energy.sum(axis = 1)
    return energy.energy
    # return energy

def main():
    if not os.path.exists('plots'):
        os.mkdir('plots')

    root = '../chappie-data/fse2020'

    benchs = np.sort(os.listdir('../chappie-data/fse2020/nop'))
    benchs = tqdm(benchs)

    summary = []
    energy = []

    for bench in benchs:
        benchs.set_description(bench)

        if bench in ('fop', 'jme', 'kafka'):
            bench_ = bench + '10'
        else:
            bench_ = bench

        if not os.path.exists('plots/{}'.format(bench)):
            os.mkdir('plots/{}'.format(bench))

        if bench in ('fop', 'jme', 'kafka'):
            a = 20
            b = 100
        else:
            a = 2
            b = 10

        ts = np.mean([parse_timestamp(os.path.join(root, 'nop', bench, 'raw', str(k), 'time.json')) / 1000000000 for k in range(a, b)])
        total_threads = len(json.load(open(os.path.join(root, 'baseline', bench, '0', 'raw', '0', 'id.json'))))
        live_threads = np.mean([
            pd.read_csv(os.path.join(
                root, 'baseline', bench, n, 'raw', str(k), 'vm.csv'
            ), delimiter = ';').groupby('epoch').id.count().mean() for n, k in product(
                os.listdir(os.path.join(root, 'baseline', bench)), range(a, b)
        )])

        methods = len(pd.concat([pd.read_csv(os.path.join(
            root, 'baseline', bench, n, 'raw', str(k), 'method.csv'
        ), delimiter = ';') for n, k in product(
            os.listdir(os.path.join(root, 'baseline', bench)), range(a, b)
        )]).trace.unique())

        s = pd.Series(
            index = ['bench', 'methods', 'total threads', 'active threads', 'execution time(s)'],
            data = [bench_, int(methods), int(total_threads), int(live_threads), np.round(ts, 2)]
        )

        summary.append(s)

        df = pd.concat([
            pd.read_csv(os.path.join(root, 'baseline', bench, i, 'summary', 'component.csv')).assign(i = i) for i in os.listdir(os.path.join(root, 'baseline', bench))
        ]).groupby(['socket', 'i']).sum().groupby('socket').agg(('mean', 'std')).stack(0).reset_index()
        df.columns = ['socket', 'component', 'mean', 'std']
        df['bench'] = bench_
        df['component'] = df['component'].str.replace('package', 'cpu')
        df = df.set_index(['bench', 'component', 'socket'])

        if bench in ('fop10', 'jme10', 'kafka10'):
            df = 10 * df
        # print(df)

        energy.append(df)

    summary = pd.concat(summary, axis = 1).T.set_index('bench')
    # summary.to_csv('summary.csv')
    print(summary)

    summary = summary.reset_index().transform(lambda x: x.map('{} & '.format)).sum(axis = 1).str[:-1].map(lambda x: x[:-1] + ' \\\\\n')
    table = summary.values

    with open('plots/summary-table.tex', 'w') as f:
        [f.write(row) for row in table]

    energy = pd.concat(energy)
    print(energy)

    energy_plot(energy.unstack().unstack())

    plt.savefig('plots/energy.pdf', bbox_inches = 'tight')
    plt.close()

#     # runtime plot
#     ax = summary['runtime'].reset_index().groupby(['bench', 'case'])[['mean', 'std']].max().unstack().plot.bar(
#         y = 'mean', yerr = 'std',
#         # color = [u'#d62728', u'#2ca02c'],
#         color = ['red', 'green', 'orange', 'blue', 'purple'],
#         figsize = (22, 10)
#     )
#
#     ax.legend(loc = 'upper left', fontsize = 20)
#
#     plt.xlabel('Benchmark', fontsize = 20)
#     plt.ylabel('Runtime (s)', fontsize = 20)
#
#     plt.xticks(fontsize = 20, rotation = 30)
#     plt.yticks(fontsize = 24)
#
#     plt.savefig('runtime-summary.pdf', bbox_inches = 'tight')
#     plt.close()
#
#     # energy plot
#     ax = summary['energy'].unstack(0).unstack(0).plot.bar(
#         y = 'mean', yerr = 'std',
#         # color = [u'#d62728', u'#2ca02c'] * 2,
#         color = ['red', 'green', 'orange', 'blue', 'purple'] * 2,
#         figsize = (22, 10)
#     )
#
#     handles, _ = ax.get_legend_handles_labels()
#     ax.legend(handles[:5], ['1ms', '2ms', '4ms', '8ms', 'nop'], loc = 'upper left', fontsize = 20)
#
#     # for rect, socket in zip(ax.patches[:len(summary)] + ax.patches[:len(summary)], (0, 0, 1, 1)):
#     #     height = rect.get_height()
#     #     text = ax.text(
#     #         rect.get_x() + rect.get_width(), rect.get_y(),
#     #         socket,
#     #         ha='center', va='bottom', fontsize = 28, color = 'white'
#     #     )
#     #     text.set_path_effects([
#     #         path_effects.Stroke(linewidth = 5, foreground = 'black'),
#     #         path_effects.Normal()
#     #     ])
#
#     plt.xlabel('Benchmark', fontsize = 20)
#     plt.ylabel('Energy (J)', fontsize = 20)
#
#     plt.xticks(fontsize = 20, rotation = 30)
#     plt.yticks(fontsize = 24)
#
#     plt.savefig('energy-summary.pdf', bbox_inches = 'tight')
#     plt.close()
#
#     # power plot
#     power = summary['energy', 'mean'] / summary['runtime', 'mean']
#     power = pd.concat([
#         summary['energy', 'mean'] / summary['runtime', 'mean'],
#         np.sqrt((summary['energy', 'std'] / summary['energy', 'mean'])**2 + (summary['energy', 'std'] / summary['runtime', 'mean'])**2),
#     ], axis = 1)
#     power.columns = ['mean', 'std']
#     ax = power.unstack(0).unstack(0).plot.bar(
#         y = 'mean', yerr = 'std',
#         # color = [u'#d62728', u'#2ca02c'] * 2,
#         color = ['red', 'green', 'orange', 'blue', 'purple'] * 2,
#         figsize = (22, 10)
#     ) # , stacked = True)
#
#     handles, _ = ax.get_legend_handles_labels()
#     ax.legend(handles[:5], ['1ms', '2ms', '4ms', '8ms', 'nop'], loc = 'upper left', fontsize = 20)
#
#     # for rect, socket in zip(ax.patches[:2] + ax.patches[4:6], (0, 0, 1, 1)):
#     #     height = rect.get_height()
#     #     text = ax.text(
#     #         rect.get_x() + rect.get_width(), rect.get_y(),
#     #         socket,
#     #         ha='center', va='bottom', fontsize = 28, color = 'white'
#     #     )
#     #     text.set_path_effects([
#     #         path_effects.Stroke(linewidth = 5, foreground = 'black'),
#     #         path_effects.Normal()
#     #     ])
#
#     plt.xlabel('Benchmark', fontsize = 20)
#     plt.ylabel('Power (W)', fontsize = 20)
#
#     plt.xticks(fontsize = 20, rotation = 30)
#     plt.yticks(fontsize = 24)
#
#     plt.savefig('power-summary.pdf', bbox_inches = 'tight')
#     plt.close()

if __name__ == '__main__':
    main()
