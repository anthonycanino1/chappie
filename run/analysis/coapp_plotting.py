#!/usr/bin/python3

import argparse
import os

from time import time

import matplotlib.pyplot as plt
import numpy as np
import pandas as pd

if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('-path', default = "chappie_test")
    parser.add_argument('-destination', default = None)
    args = parser.parse_args()

    suite = args.path.split(os.sep)[-1]

    # setup the paths
    if not os.path.exists(args.path):
        raise FileNotFoundError('No directory at {}'.format(os.path.abspath(args.path)))

    if args.destination is None:
        args.destination = os.path.join(args.path, 'plots')

    if not os.path.exists(args.destination):
        os.mkdir(args.destination)

    start = time()

    benchmarks = [benchmark for benchmark in os.listdir(args.path) if benchmark != 'plots']

    summary = {benchmark: pd.read_csv(os.path.join(args.path, benchmark, 'summary', 'chappie.component.csv')) for benchmark in benchmarks}
    correlation = {benchmark: pd.read_csv(os.path.join(args.path, benchmark, 'summary', 'chappie.correlation.csv')) for benchmark in benchmarks}

    for benchmark in benchmarks:
        summary[benchmark] = summary[benchmark].groupby('order').sum().sum(axis = 1).to_frame('energy')
        summary[benchmark]['benchmark'] = benchmark
        correlation[benchmark] = correlation[benchmark].sort_values(['level', 'type', 'value'])

        summary[benchmark] = pd.merge(summary[benchmark].reset_index(), correlation[benchmark].iloc[:2][['order', 'Correlation']], on = 'order')
        summary[benchmark].columns = summary[benchmark].columns.str.title()

    summary = pd.concat(summary.values())
    colors = [
        'grey', 'darkgray',
        'palegreen', 'seagreen', 'lawngreen', 'forestgreen', 'lightsteelblue', 'cornflowerblue', 'indianred', 'maroon',
        'palegreen', 'seagreen', 'lawngreen', 'forestgreen', 'lightsteelblue', 'cornflowerblue', 'indianred', 'maroon'
    ]

    print(summary)

    ax = None
    first = True
    for order, color, width in zip((1, 2), ('r', 'b'), (-0.125, 0.125)):
        bench = summary[summary['Order'] == order][['Energy', 'Benchmark']]
        ax = bench.plot.bar(
            x = 'Benchmark',
            stacked = True,
            color = color,
            edgecolor = 'black',
            linewidth = 0.125,
            align = 'edge',
            width = width,
            ax = ax,
            figsize = (16, 9),
            legend = False,
        )

        for patch, corr in zip(ax.patches, summary[summary['Order'] == order]['Correlation']):
            ax.text(
                patch.get_x() + (0.05 if order == 2 else -0.10),
                patch.get_height() + 5,
                '{:.2f}'.format(corr),
                fontsize = 9
            )

    handles, labels = ax.get_legend_handles_labels()
    plt.legend(handles, labels, loc = 'best', prop = {'size': 7})

    max_tick = np.ceil(summary.drop(columns = ['Benchmark', 'Correlation']).sum(axis = 1).astype(int).max() / 100 + 1) * 100

    print(benchmarks)
    plt.xticks(fontsize = 8, rotation = 30)
    plt.yticks(ticks = np.arange(0, max_tick, 100), fontsize = 8)
    plt.xlabel('Benchmarks', fontsize = 12)
    plt.ylabel('Energy (J)', fontsize = 12)

    plt.savefig(os.path.join(args.destination, '{}_energy_attribution_by_component.svg'.format(suite)), bbox_inches = 'tight')

    print('{:0.2f} seconds for plotting'.format(time() - start))

    try:
        plt.show()
    except:
        print('No visual front end; skipping plt.show()')
