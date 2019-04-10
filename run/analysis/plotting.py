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
    method = {benchmark: pd.read_csv(os.path.join(args.path, benchmark, 'summary', 'chappie.method.csv')) for benchmark in benchmarks}

    for benchmark in benchmarks:
        if not os.path.exists(os.path.join(args.destination, benchmark)):
            os.mkdir(os.path.join(args.destination, benchmark))
        # import matplotlib.pyplot as plt
        # for col in ('all', 'unfiltered', 'deep'):
        #     col_df = method[benchmark][method[benchmark].type == col]
        #
        #     df = col_df[col_df.level == 'context']
        #     df['method'] = df['name'].str.split(';').map(lambda x: x[0])
        #     df['context'] = df['name'].str.split(';').map(lambda x: x[1] if len(x) > 1 else 'end')
        #     top = df.groupby('method')['Energy'].sum().head(10)
        #
        #     for md, group in df.groupby('method'):
        #         if md in top:
        #             group['Energy'] /= group['Energy'].sum()
        #             group['Energy'] = group['Energy'].fillna(1)
        #             group.plot(title = md, y = 'Energy', kind = 'pie', labels = None, figsize = (4, 4))
        #             plt.legend(labels = group['context'], prop = {'size': 9}, loc = 2)
        #
        #             md = md.replace('<', '')
        #             md = md.replace('>', '')
        #             plt.savefig(os.path.join(args.destination, benchmark, '{}_{}_context.svg'.format(md, col)), bbox_inches = 'tight')
        #             plt.close()
        #
        #     for type in ('method', 'class', 'package'):
        #         df = col_df[col_df.level == type].sort_values('Energy', ascending = False).head(10)
        #
        #         x = np.array(df['name'])
        #         if type == 'method':
        #             ax = df.plot(kind='barh', x='name', y=['Energy','Time'], width=0.3, figsize=(6, 3.5))
        #         else:
        #             ax = df.plot(kind='barh', x='name', y='Energy', width=0.3, color = 'tab:blue', figsize=(6, 3.5))
        #
        #         ax.set_yticklabels([])
        #         ax.invert_yaxis()
        #         ax.tick_params(
        #             axis='y', # changes apply to the x-axis
        #             which='both', # both major and minor ticks are affected
        #             bottom=False, # ticks along the bottom edge are off
        #             top=False, # ticks along the top edge are off
        #             labelbottom=False # labels along the bottom edge are off
        #         )
        #
        #         for i, v in enumerate(x):
        #             try:
        #                 ax.text(0, i - .20, str('  ' + v), color='black', fontsize = 8)
        #             except:
        #                 pass
        #
        #         plt.ylabel('',fontsize=12)
        #         plt.xlabel('',fontsize=12)
        #         plt.tight_layout()
        #
        #         plt.savefig(os.path.join(args.destination, benchmark, '{}_{}_bar.svg'.format(col, type)), bbox_inches = 'tight')
        #         plt.close()

        summary[benchmark] = summary[benchmark].drop(columns = ['total package', 'total dram']).rename(columns = {
            'other application package': 'other package',
            'other application dram': 'other dram',
            'system package': 'jvm-c package',
            'system dram': 'jvm-c dram',
            'jvm package': 'jvm-java package',
            'jvm dram': 'jvm-java dram',
        })
        summary[benchmark]['benchmark'] = benchmark

        runtime[benchmark]['benchmark'] = benchmark
        runtime[benchmark]['mean'] = runtime[benchmark]['mean'][runtime[benchmark]['experiment'] == 'reference'].max()

    # energy summary
    socket_summary = pd.concat(summary).groupby(['benchmark', 'socket']).sum().reset_index().sort_values('benchmark')

    ax = None
    first = True
    for socket, color, width in zip((1, 2), ('Reds', 'Blues'), (-0.125, 0.125)):
        soc = socket_summary[socket_summary['socket'] == socket].drop(columns = 'socket')
        print(soc)
        ax = soc.plot.bar(
            x = 'benchmark',
            stacked = True,
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

    plt.savefig(os.path.join(args.destination, 'attribution.svg'.format(suite)), bbox_inches = 'tight')

    # overhead
    runtime = pd.concat(runtime.values())
    runtime = runtime[runtime['experiment'] != 'reference'].sort_values('benchmark')[['benchmark', 'mean', 'std', 'overhead', 'error']]
    runtime.columns = ['benchmark', 'base runtime', 'deviation', 'overhead', 'error']
    runtime[['overhead', 'error']] *= 100
    runtime['overhead'] = runtime['overhead'].map('{:.2f}%'.format)
    runtime['error'] = runtime['error'].transform('{:.2f}%'.format)
    runtime['base runtime'] /= 10**9
    runtime['base runtime'] = runtime['base runtime'].map('{:.2f} s'.format)
    runtime['deviation'] /= 10**9
    runtime['deviation'] = runtime['deviation'].map('{:.2f} s'.format)
    runtime.columns = runtime.columns.str.title()
    print(runtime)
    runtime = runtime.to_latex(
        index = False,
        # float_format = '{:.2f}%'.format,
        column_format = '|l|r|r|r|r|'
    )
    runtime = '\documentclass{article}\n\\usepackage{booktabs}\n\\begin{document}\n' + runtime + '\end{document}'
    pdf = build_pdf(runtime)
    pdf.save_to(os.path.join(args.destination, 'overhead.pdf'))

    print('{:.2f} seconds for plotting'.format(time() - start))

    try:
        pass
        # plt.show()
    except:
        print('No visual front end; skipping plt.show()')
