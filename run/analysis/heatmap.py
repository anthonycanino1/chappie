#!/usr/bin/python3

import argparse
import json
import os

from time import time
import xml.etree.ElementTree as ET

import matplotlib
matplotlib.use('Agg')

import matplotlib.pyplot as plt
import numpy as np
import pandas as pd
import seaborn as sns

from latex import build_pdf
from tabulate import tabulate

def parse_args():
    parser = argparse.ArgumentParser()
    parser.add_argument('-config')

    return parser.parse_args()

def parse_configs(config):
    configs = []
    for case in os.listdir(config):
        if 'NOP' not in case:
            configs.append({})
            root = ET.parse(os.path.join(args.config, case)).getroot()
            for child in root:
                try:
                    configs[-1][child.tag] = int(child.text)
                except:
                    configs[-1][child.tag] = child.text

    return configs

if __name__ == '__main__':
    args = parse_args()
    configs = parse_configs(args.config)

    path = os.path.dirname(args.config)

    runtime = []
    for config in configs:
        df = pd.read_csv(os.path.join(config['workPath'], 'summary', 'chappie.runtime.csv')).head(1)

        df['rate'] = config['timerRate']
        df['vm'] = config['timerRate'] * config['vmFactor']
        df['os'] = config['osFactor']
        # df['vm'] = config['timerRate'] * config['vmFactor']
        # df['hp'] = config['timerRate'] * config['hpFactor']
        df['case'] = config['workPath'].split(os.sep)[-1]

        runtime.append(df)

    runtime = pd.concat(runtime)

    runtime_grid = runtime.pivot(index = 'os', columns = 'vm', values = 'time_overhead').sort_index(ascending = False)
    runtime_grid.index.name = 'vm rate (ms)'
    runtime_grid.columns.name = 'os factor'

    sns.heatmap(runtime_grid, cmap = 'Reds')
    plt.savefig(os.path.join(path, 'runtime_map.svg'), bbox_inches = 'tight')
    # plt.xticks(os_rates)

    plt.figure()
    energy_grid = runtime.pivot(index = 'os', columns = 'vm', values = 'energy_overhead').sort_index(ascending = False)
    energy_grid.index.name = 'vm rate (ms)'
    energy_grid.columns.name = 'os factor'

    sns.heatmap(energy_grid, cmap = 'Greens')
    plt.savefig(os.path.join(path, 'energy_map.svg'), bbox_inches = 'tight')
    # plt.xticks(os_rates)

    # runtime[['overhead', 'error']] *= 100
    # runtime['overhead'] = runtime['overhead'].map('{:.2f}%'.format)
    # runtime['error'] = runtime['error'].transform('{:.2f}%'.format)
    # runtime['runtime'] /= 10**3
    # runtime['runtime'] = runtime['runtime'].map('{:.2f} s'.format)
    # runtime['deviation'] /= 10**3
    # runtime['deviation'] = runtime['deviation'].map('{:.2f} s'.format)
    # runtime.columns = runtime.columns.str.title()
    # # print(runtime)
    # runtime = runtime.to_latex(
    #     index = False,
    #     # float_format = '{:.2f}%'.format,
    #     column_format = '|l|r|r|r|r|'
    # )
    # runtime = '\documentclass{article}\n\\usepackage{booktabs}\n\\begin{document}\n' + runtime + '\end{document}'
    # pdf = build_pdf(runtime)
    # pdf.save_to(os.path.join(path, 'overhead.pdf'))

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

    methods = []
    for config in configs:
        df = pd.read_csv(os.path.join(config['workPath'], 'summary', 'chappie.method.csv'))

        df['rate'] = config['timerRate']
        df['vm'] = config['timerRate'] * config['vmFactor']
        df['os'] = config['osFactor']
        # df['os'] = config['timerRate'] * config['osFactor']
        df['hp'] = config['timerRate'] * config['hpFactor']
        df['case'] = config['workPath'].split(os.sep)[-1]
        methods.append(df)

    methods = pd.concat(methods)
    methods = methods[(methods['level'] == 'method') & (methods['type'] == 'all')]
    methods = methods.pivot_table(
        index = 'name',
        values = 'Energy',
        columns = ['os', 'vm'],
    ).corr()
    methods = methods[methods.columns[0]]
    methods.name = 'Correlation'
    methods = methods.reset_index()

    corrs_grid = methods.pivot(index = 'os', columns = 'vm', values = 'Correlation').sort_index(ascending = False)
    corrs_grid.index.name = 'vm rate (ms)'
    corrs_grid.columns.name = 'os factor'

    import seaborn as sns
    plt.figure()
    sns.heatmap(corrs_grid, cmap = 'Blues_r')
    plt.savefig(os.path.join(path, 'correlation_map.svg'), bbox_inches = 'tight')
    # plt.xticks(os_rates)

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
