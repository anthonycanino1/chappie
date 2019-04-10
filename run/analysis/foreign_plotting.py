#!/usr/bin/python3

import argparse
import os

from time import time

import matplotlib.pyplot as plt
import numpy as np
import pandas as pd

from latex import build_pdf
from tabulate import tabulate

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

    runtime = {benchmark: pd.read_csv(os.path.join(args.path, benchmark, 'summary', 'chappie.runtime.csv')) for benchmark in benchmarks}
    summary = {benchmark: pd.read_csv(os.path.join(args.path, benchmark, 'summary', 'chappie.component.csv')) for benchmark in benchmarks}
    correlation = {benchmark: pd.read_csv(os.path.join(args.path, benchmark, 'summary', 'chappie.correlation.csv')) for benchmark in benchmarks}

    for benchmark in benchmarks:
        summary[benchmark] = summary[benchmark].sum()
        summary[benchmark]['foreign load'] = summary[benchmark][3:4].sum()
        summary[benchmark]['application'] = summary[benchmark][5:].sum()
        summary[benchmark]['benchmark'] = benchmark
        summary[benchmark]['correlation'] = correlation[benchmark].iloc[6]['Correlation']
        summary[benchmark] = summary[benchmark][-4:]
        summary[benchmark].index = summary[benchmark].index.str.title()

        runtime[benchmark]['Benchmark'] = benchmark

    summary = pd.concat(summary.values(), axis = 1).T.sort_values('Benchmark')
    print(summary)

    ax = None
    first = True
    for experiment, color, width in zip(('Application', 'Foreign Load'), ('blue', 'orange'), (-0.25, 0.25)):
        bench = summary[[experiment, 'Benchmark']]
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
            legend = False
        )

        if experiment == 'Application':
            for patch, corr in zip(ax.patches, summary['Correlation']):
                ax.text(
                    patch.get_x() - 0.225,
                    patch.get_height() + 25,
                    '{:.2f}'.format(corr),
                    fontsize = 8
                )

    handles, labels = ax.get_legend_handles_labels()
    plt.legend(handles, labels, loc = 'best', prop = {'size': 7})

    max_tick = np.ceil(summary.drop(columns = ['Benchmark', 'Correlation']).sum(axis = 1).astype(int).max() / 100) * 100

    plt.xticks(fontsize = 8, rotation = 30)
    plt.yticks(ticks = np.arange(0, max_tick, 100), fontsize = 8)
    plt.xlabel('Benchmarks', fontsize = 12)
    plt.ylabel('Energy (J)', fontsize = 12)

    plt.savefig(os.path.join(args.destination, 'attribution.svg'), bbox_inches = 'tight')

    # overhead
    runtime = pd.concat(runtime.values())
    runtime = runtime[runtime['experiment'] != 'reference'].sort_values('Benchmark')[['Benchmark', 'mean', 'std', 'overhead', 'error']]
    runtime.columns = ['benchmark', 'base runtime', 'deviation', 'overhead', 'error']
    runtime[['overhead', 'error']] *= 100
    runtime['overhead'] = runtime['overhead'].map('{:.2f}%'.format)
    runtime['error'] = runtime['error'].transform('{:.2f}%'.format)
    runtime['base runtime'] /= 10**9
    runtime['base runtime'] = runtime['base runtime'].map('{:.2f} s'.format)
    runtime['deviation'] /= 10**9
    runtime['deviation'] = runtime['deviation'].map('{:.2f} s'.format)
    runtime.columns = runtime.columns.str.title()
    runtime = runtime
    print(runtime)
    runtime = runtime.to_latex(
        index = False,
        # float_format = '{:.2f}%'.format,
        column_format = '|l|r|r|r|r|'
    )
    runtime = '\documentclass{article}\n\\usepackage{booktabs}\n\\begin{document}\n' + runtime + '\end{document}'
    pdf = build_pdf(runtime)
    pdf.save_to(os.path.join(args.destination, 'overhead.pdf'))

    print('{:0.2f} seconds for plotting'.format(time() - start))

    try:
        plt.show()
    except:
        print('No visual front end; skipping plt.show()')
