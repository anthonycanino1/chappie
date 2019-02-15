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

    args.path = os.path.join(args.path)

    benchmarks = [benchmark for benchmark in os.listdir(args.path) if benchmark != 'plots']

    summary = {benchmark: pd.read_csv(os.path.join(args.path, benchmark, 'summary', 'chappie.component.csv')) for benchmark in benchmarks}
    method = {benchmark: pd.read_csv(os.path.join(args.path, benchmark, 'summary', 'chappie.method.csv')) for benchmark in benchmarks}

    for benchmark in benchmarks:
        if not os.path.join(args.destination, benchmark):
            os.mkdir(os.path.join(args.destination, benchmark))
        import matplotlib.pyplot as plt
        for col in ('all', 'unfiltered', 'deep'):
            col_df = method[benchmark][method[benchmark].type == col]

            df = col_df[col_df.level == 'context']
            df['method'] = df['name'].str.split(';').map(lambda x: x[0])
            df['context'] = df['name'].str.split(';').map(lambda x: x[1])
            top = df.groupby('method')['Energy'].sum().sort_values().head(3).index.values

            for md, group in df.groupby('method'):
                if md in top:
                    group.plot(title = md, kind='pie', labels=None, y='Energy',figsize=(4, 4))

            for type in ('method', 'class', 'package'):
                df = col_df[col_df.level == type]

                x = np.array(df['name'])
                if type == 'method':
                    ax = df.plot(kind='barh', x='name', y=['Energy','Time'], width=0.3, figsize=(6, 3.5))
                else:
                    ax = df.plot(kind='barh', x='name', y='Energy', width=0.3, color = 'tab:blue', figsize=(6, 3.5))

                ax.set_yticklabels([])
                ax.invert_yaxis()
                ax.tick_params(
                    axis='y',  # changes apply to the x-axis
                    which='both',  # both major and minor ticks are affected
                    bottom=False,  # ticks along the bottom edge are off
                    top=False,  # ticks along the top edge are off
                    labelbottom=False)  # labels along the bottom edge are off

                for i, v in enumerate(x):
                    ax.text(0, i - .20, str('  ' + v), color='black')

                plt.ylabel('',fontsize=12)
                plt.xlabel('',fontsize=12)
                plt.tight_layout()

                plt.savefig(os.path.join(args.destination, benchmark, '{}_{}_energy_proportion.svg'.format(col, type)), bbox_inches = 'tight')

        summary[benchmark] = summary[benchmark].drop(columns = ['total package', 'total dram'])
        summary[benchmark]['benchmark'] = benchmark

    socket_summary = pd.concat(summary).groupby(['benchmark', 'socket']).sum().reset_index()
    # print(socket_summary)

    colors = [
        'grey', 'darkgray',
        'palegreen', 'seagreen', 'lawngreen', 'forestgreen', 'lightsteelblue', 'cornflowerblue', 'indianred', 'maroon',
        'palegreen', 'seagreen', 'lawngreen', 'forestgreen', 'lightsteelblue', 'cornflowerblue', 'indianred', 'maroon'
    ]

    ax = None
    first = True
    for socket, color, width in zip((1, 2), ('Reds', 'Blues'), (-0.125, 0.125)):
        soc = socket_summary[socket_summary['socket'] == socket].drop(columns = 'socket')
        ax = soc.plot.bar(
            x = 'benchmark',
            stacked = True,
            # color = colors,
            cmap = 'tab20',
            edgecolor = 'black',
            linewidth = 0.125,
            align = 'edge',
            width = width,
            ax = ax,
            figsize = (16, 9),
            legend = False
        )

        if first:
            first = False
            handles, labels = ax.get_legend_handles_labels()

            handles.reverse()
            labels.reverse()

            plt.legend(handles, labels, loc = 'best', prop = {'size': 7})

    max_tick = np.ceil(socket_summary.drop(columns = ['benchmark']).sum(axis = 1).astype(int).max() / 100) * 100

    plt.xticks(fontsize = 8, rotation = 30)
    plt.yticks(ticks = np.arange(0, max_tick, 100), fontsize = 8)
    plt.xlabel('Benchmarks', fontsize = 12)
    plt.ylabel('Energy (J)', fontsize = 12)

    plt.savefig(os.path.join(args.destination, '{}_energy_attribution_by_component.svg'.format(suite)), bbox_inches = 'tight')

    print('{:.2f} seconds for plotting'.format(time() - start))

    try:
        plt.show()
    except:
        print('No visual front end; skipping plt.show()')
