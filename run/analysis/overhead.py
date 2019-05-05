#!/usr/bin/python3

import argparse
import os

from time import time

import matplotlib
matplotlib.use('Agg')

import matplotlib.pyplot as plt
import numpy as np
import pandas as pd

from latex import build_pdf
from tabulate import tabulate

if __name__ == '__main__':
    path = os.getcwd()

    runs = [experiment for experiment in os.listdir(path) if experiment != 'reference' and experiment != 'temp' and experiment != 'dacapo' and r'.' not in experiment]

    runtime = pd.concat([
        pd.concat([
            pd.read_csv(os.path.join(path, run, '1', 'sunflow', str(i), 'summary', 'chappie.runtime.csv')) for i in range(1,2)
            ]).groupby('experiment').mean().reset_index()
        for run in runs
    ])

    names = [[name, 'reference'] for name in runs]
    runtime['experiment'] = [x for y in names for x in y]

    # print(runtime)
    runtime = runtime.sort_values('mean').drop_duplicates(subset = ['experiment']).sort_values('experiment')
    # runtime = runtime.sort_index(ascending = False)
    # print(runtime)

    runtime.columns = ['experiment', 'runtime', 'deviation', 'overhead', 'error']

    runtime2 = runtime[runtime['experiment'] != 'reference']

    runtime2['vm'] = runtime2.experiment.map(lambda x: x.split('_')[0]).astype(int)
    runtime2['os'] = runtime2.experiment.map(lambda x: x.split('_')[1]).astype(int)
    runtime2['hp'] = runtime2.experiment.map(lambda x: x.split('_')[2]).astype(int)
    runtime2 = runtime2[runtime2['vm'] == runtime2['hp']]
    runtime2['os'] *= runtime2['vm']
    runtime2['os'] = runtime2['os'].map(lambda x: 20 if x == 16 else x)
    print(runtime2)
    # print(runtime_grid)

    runtime_grid = runtime2.pivot(index = 'vm', columns = 'os', values = 'overhead').sort_index(ascending = False)

    import seaborn as sns
    sns.heatmap(runtime_grid, cmap = 'Reds')
    plt.savefig(os.path.join(path, 'runtime_map.svg'), bbox_inches = 'tight')

    runtime[['overhead', 'error']] *= 100
    runtime['overhead'] = runtime['overhead'].map('{:.2f}%'.format)
    runtime['error'] = runtime['error'].transform('{:.2f}%'.format)
    runtime['runtime'] /= 10**3
    runtime['runtime'] = runtime['runtime'].map('{:.2f} s'.format)
    runtime['deviation'] /= 10**3
    runtime['deviation'] = runtime['deviation'].map('{:.2f} s'.format)
    runtime.columns = runtime.columns.str.title()
    # print(runtime)
    runtime = runtime.to_latex(
        index = False,
        # float_format = '{:.2f}%'.format,
        column_format = '|l|r|r|r|r|'
    )
    runtime = '\documentclass{article}\n\\usepackage{booktabs}\n\\begin{document}\n' + runtime + '\end{document}'
    pdf = build_pdf(runtime)
    pdf.save_to(os.path.join(path, 'overhead.pdf'))

    # summary = {run: pd.read_csv(os.path.join(path, run, 'dacapo', 'h2', 'summary', 'chappie.component.csv')) for run in runs}
    #
    # for run in runs:
    #     summary[run] = summary[run].drop(columns = ['total package', 'total dram']).rename(columns = {
    #         'other application package': 'other package',
    #         'other application dram': 'other dram',
    #         'system package': 'jvm-c package',
    #         'system dram': 'jvm-c dram',
    #         'jvm package': 'jvm-java package',
    #         'jvm dram': 'jvm-java dram',
    #     })
    #     summary[run]['experiment'] = run
    #
    # # energy summary
    # socket_summary = pd.concat(summary).groupby(['experiment', 'socket']).sum().reset_index().sort_values('experiment')
    #
    # ax = None
    # first = True
    # for socket, color, width in zip((1, 2), ('Reds', 'Blues'), (-0.125, 0.125)):
    #     soc = socket_summary[socket_summary['socket'] == socket].drop(columns = 'socket')
    #     # print(soc)
    #     soc = soc.drop(columns = ['other package', 'other dram'])
    #     ax = soc.plot.bar(
    #         x = 'experiment',
    #         stacked = True,
    #         cmap = 'tab20',
    #         edgecolor = 'black',
    #         linewidth = 0.125,
    #         align = 'edge',
    #         width = width,
    #         ax = ax,
    #         figsize = (16, 9),
    #         legend = False
    #     )
    #
    #     if first:
    #         first = False
    #         handles, labels = ax.get_legend_handles_labels()
    #
    #         handles.reverse()
    #         labels.reverse()
    #
    #         plt.legend(handles, labels, loc = 'best', prop = {'size': 7})
    #
    # max_tick = np.ceil(socket_summary.drop(columns = ['other package', 'other dram']).drop(columns = ['experiment']).sum(axis = 1).astype(int).max() / 100) * 100
    #
    # plt.xticks(fontsize = 8, rotation = 30)
    # plt.yticks(ticks = np.arange(0, max_tick, 100), fontsize = 8)
    # plt.xlabel('Benchmarks', fontsize = 12)
    # plt.ylabel('Energy (J)', fontsize = 12)
    #
    # plt.savefig(os.path.join(path, 'attribution.svg'), bbox_inches = 'tight')
    #
    # plt.figure()

    methods = [
        pd.concat([
            pd.read_csv(os.path.join(path, run, '1', 'sunflow', str(i), 'summary', 'chappie.method.csv')) for i in range(1,2)
        ]).rename(columns = {'Energy': run}) for run in runs
    ]

    # methods = [pd.read_csv(os.path.join(path, run, 'dacapo', 'h2', 'summary', 'chappie.method.csv')).rename(columns = {'Energy': run}) for run in runs]
    df = methods[0]
    df = df[(df['level'] == 'method') & (df['type'] == 'all')].drop(columns = ['level', 'type', 'context', 'Time']).groupby('name').mean().reset_index()
    for m in methods[1:]:
        m = m[(m['level'] == 'method') & (m['type'] == 'all')].drop(columns = ['level', 'type', 'context', 'Time']).groupby('name').mean().reset_index()
        df = pd.merge(df, m, on = 'name', how = 'outer')

    corrs = df.corr()[['1_1_1']]
    corrs2 = corrs.sort_index().reset_index()
    print(corrs2)

    corrs2['vm'] = corrs2['index'].map(lambda x: x.split('_')[0]).astype(int)
    corrs2['os'] = corrs2['index'].map(lambda x: x.split('_')[1]).astype(int)
    corrs2['hp'] = corrs2['index'].map(lambda x: x.split('_')[2]).astype(int)
    corrs2 = corrs2[corrs2['vm'] == corrs2['hp']]
    corrs2['os'] *= corrs2['vm']
    corrs2['os'] = corrs2['os'].map(lambda x: 20 if x == 16 else x)
    # print(corrs2)

    # print(corrs2.sort_values(['vm', 'os']))
    corrs_grid = corrs2.pivot(index = 'vm', columns = 'os', values = '1_1_1').sort_index(ascending = False)

    import seaborn as sns
    plt.figure()
    sns.heatmap(corrs_grid, cmap = 'Blues_r')
    plt.savefig(os.path.join(path, 'correlation_map.svg'), bbox_inches = 'tight')

    # corrs.name = 'Correlation'
    # corrs.index.name = 'Experiment'
    #
    # corrs = corrs.to_latex(
    #     # float_format = '{:.2f}%'.format,
    #     column_format = '|l|r|'
    # )
    # corrs = '\documentclass{article}\n\\usepackage{booktabs}\n\\begin{document}\n' + corrs + '\end{document}'
    # pdf = build_pdf(corrs)
    # pdf.save_to(os.path.join(path, 'correlation.pdf'))
