#!/usr/bin/python3

import argparse
import json

import xml.etree.ElementTree as ET

import matplotlib
matplotlib.use('Agg')

import matplotlib.pyplot as plt
import seaborn as sns

from summarize import *

def parse_args():
    parser = argparse.ArgumentParser()
    parser.add_argument('-benchmark')

    return parser.parse_args()


def filter_to_application(l):
    if l == 'end':
        return l
    else:
        while len(l) > 1:
            if 'java' in l[0]:
                l.pop(0)
            else:
                break

        return l

def main():
    args = parse_args()

    if not os.path.exists(os.path.join(args.benchmark, 'plots')):
        os.mkdir(os.path.join(args.benchmark, 'plots'))

    method = pd.read_csv(os.path.join(args.benchmark, 'summary', 'chappie.method.csv'), index_col = 'benchmark').dropna()

    method['tr'] = method.index.str.split('_').map(lambda x: x[0] if len(x) > 1 else x[0]).str.findall('\d+').map(lambda x: x[0]).astype(int)
    method['vm'] = method.index.str.split('_').map(lambda x: x[1] if len(x) > 1 else x[0]).str.findall('\d+').map(lambda x: x[0]).astype(int) * method['tr']
    method['os'] = method.index.str.split('_').map(lambda x: x[2] if len(x) > 1 else [0]).str.findall('\d+').map(lambda x: x[0] if x == x else '0').astype(int) * method['tr']
    method = method[(method.vm == 4) & (method.os == 10)]

    method['all'] = method['stack'].str.split(';').map(lambda x: x[:2])
    method['unfiltered'] = method['stack'].str.split(';').map(lambda x: x[:2])
    method['deep'] = method['stack'].str.split(';').map(filter_to_application).map(lambda x: x[:2])

    method['all_context'] = method['all'].map(lambda x: x[1])
    method['unfiltered_context'] = method['unfiltered'].map(lambda x: x[1])
    method['deep_context'] = method['deep'].map(lambda x: x[1] if len(x) > 1 else 'end')

    method['all'] = method['all'].map(lambda x: x[0])
    method['unfiltered'] = method['unfiltered'].map(lambda x: x[0])
    method['deep'] = method['deep'].map(lambda x: x[0])

    method.energy /= method.energy.sum()
    method.hits /= method.hits.sum()

    top = {}
    for col in ('all', 'unfiltered', 'deep'):
        df = method.groupby(col).sum()

        df['method'] = df.index.str.split('.').map(lambda x: x[-1] if len(x) > 1 else x[0])
        df['class'] = df.index.str.split('.').map(lambda x: x[-2] if len(x) > 1 else x[0])
        df['package'] = df.index.str.split('.').map(lambda x: '.'.join(x[:-2]) if len(x) > 1 else x[0])

        df = df[~df.package.str.contains(r'chappie.')]
        if col != 'unfiltered':
            df = df[~df.package.str.contains(r'java.')]

        for col2 in ('method', 'class', 'package'):
            df2 = df.groupby(col2).sum().sort_values('energy')
            if col2 == 'method':
                # print(col2)
                df2.tail(10).plot(kind = 'barh', y = ['energy', 'hits'], width = 0.3)
            else:
                df2.tail(10).plot(kind = 'barh', y = 'energy', width = 0.3)
            top[col, col2] = df2.tail(3).index
            plt.savefig(os.path.join(args.benchmark, 'plots', '{}_{}_ranking.pdf'.format(col, col2)), bbox_inches = 'tight')

    # sys.exit(0)
    for col in ('all', 'unfiltered', 'deep'):
        df = method.groupby([col, '{}_context'.format(col)]).sum().reset_index()

        df['method'] = df[col].str.split('.').map(lambda x: x[-1] if len(x) > 1 else x[0])
        df['class'] = df[col].str.split('.').map(lambda x: x[-2] if len(x) > 1 else x[0])
        df['package'] = df[col].str.split('.').map(lambda x: '.'.join(x[:-2])  if len(x) > 1 else x[0])

        df['method_context'] = df['{}_context'.format(col)].str.split('.').map(lambda x: x[-1] if len(x) > 1 else x[0])
        df['class_context'] = df['{}_context'.format(col)].str.split('.').map(lambda x: x[-2] if len(x) > 1 else x[0])
        df['package_context'] = df['{}_context'.format(col)].str.split('.').map(lambda x: '.'.join(x[:-2])  if len(x) > 1 else x[0])
        if col != 'unfiltered':
            df = df[~(df.package.str.contains(r'java.') | df.package.str.contains(r'chappie.'))]

        for col2 in ('method', 'class', 'package'):
            for m in top[col, col2]:
                df2 = df[df[col2] == m]
                df2.energy /= df2.energy.sum()
                df2 = df2[df2.energy > 0]
                df2 = df2.groupby('{}_context'.format(col2)).energy.sum()
                # df2.index = df2['{}_context'.format(col2)]
                # df2 = df2.energy
                print(df2)

                df2.plot(kind = 'pie', title = m, labels = None)
                plt.ylabel('')
                plt.legend(df2.index)

                plt.savefig(os.path.join(args.benchmark, 'plots', '{}_{}_{}_context.pdf'.format(m.replace('<', '').replace('>', ''), col, col2)), bbox_inches = 'tight', legend = True)

                # df2 = df[df[col2] == top[col, col2]]
                # df2 = df2.groupby([col2, '{}_context'.format(col2)]).energy.sum().sort_values()
                # print(df2.tail(10))
                # df2.tail(10).plot(kind = 'barh', width = 0.3)

            # df.tail(10).plot(kind = 'barh', width = 0.3)
            # plt.savefig(os.path.join(path, 'plots', '{}_ranking.svg'.format(col2)), bbox_inches = 'tight')

    # ax = df.plot(kind='barh', x='name', y=['Energy','Time'], width=0.3, figsize=(6, 3.5))

    # all_method.groupby('')

    # print(all_method)
    # sys.exit(0)
    #
    #
    #
    # benchmarks = [benchmark for benchmark in os.listdir(args.path) if benchmark != 'plots']
    #
    # runtime = {benchmark: pd.read_csv(os.path.join(args.path, benchmark, 'summary', 'chappie.runtime.csv')) for benchmark in benchmarks}
    # summary = {benchmark: pd.read_csv(os.path.join(args.path, benchmark, 'summary', 'chappie.component.csv')) for benchmark in benchmarks}
    # method = {benchmark: pd.read_csv(os.path.join(args.path, benchmark, 'summary', 'chappie.method.csv')) for benchmark in benchmarks}
    #
    # for benchmark in benchmarks:
    #     if not os.path.exists(os.path.join(args.destination, benchmark)):
    #         os.mkdir(os.path.join(args.destination, benchmark))
    #     import matplotlib.pyplot as plt
    #     for col in ('all', 'unfiltered', 'deep'):
    #         col_df = method[benchmark][method[benchmark].type == col]
    #
    #         df = col_df[col_df.level == 'context']
    #         df['method'] = df['name'] # .str.split(';').map(lambda x: x[0])
    #         # df['context'] = df['name'].str.split(';').map(lambda x: x[1] if len(x) > 1 else 'end')
    #         top = df.groupby('method')['Energy'].sum().head(10)
    #
    #         for md, group in df.groupby('method'):
    #             if md in top:
    #                 # print(group)
    #                 group['Energy'] /= group['Energy'].sum()
    #                 group['Energy'] = group['Energy'].fillna(1)
    #                 group.plot(title = md, y = 'Energy', kind = 'pie', labels = None, figsize = (4, 4))
    #                 plt.legend(labels = group['context'], prop = {'size': 9}, loc = 2)
    #
    #                 md = md.replace('<', '')
    #                 md = md.replace('>', '')
    #                 plt.savefig(os.path.join(args.destination, benchmark, '{}_{}_context.svg'.format(md, col)), bbox_inches = 'tight')
    #                 plt.close()
    #
    #         for type in ('method', 'class', 'package'):
    #             df = col_df[col_df.level == type].sort_values('Energy', ascending = False).head(10)
    #
    #             x = np.array(df['name'])
    #             if type == 'method':
    #                 df[['type', 'name', 'Energy']].to_csv(os.path.join(args.destination, benchmark, '{}_{}.csv'.format(col, type)), index = False)
    #                 ax = df.plot(kind='barh', x='name', y=['Energy','Time'], width=0.3, figsize=(6, 3.5))
    #             else:
    #                 ax = df.plot(kind='barh', x='name', y='Energy', width=0.3, color = 'tab:blue', figsize=(6, 3.5))
    #
    #             ax.set_yticklabels([])
    #             ax.invert_yaxis()
    #             ax.tick_params(
    #                 axis='y', # changes apply to the x-axis
    #                 which='both', # both major and minor ticks are affected
    #                 bottom=False, # ticks along the bottom edge are off
    #                 top=False, # ticks along the top edge are off
    #                 labelbottom=False # labels along the bottom edge are off
    #             )
    #
    #             for i, v in enumerate(x):
    #                 try:
    #                     ax.text(0, i - .20, str('  ' + v), color='black', fontsize = 8)
    #                 except:
    #                     pass
    #
    #             plt.ylabel('',fontsize=12)
    #             plt.xlabel('',fontsize=12)
    #             plt.tight_layout()
    #
    #             plt.savefig(os.path.join(args.destination, benchmark, '{}_{}_bar.svg'.format(col, type)), bbox_inches = 'tight')
    #             plt.close()
    #
    #     summary[benchmark] = summary[benchmark].drop(columns = ['total package', 'total dram']).rename(columns = {
    #         'other application package': 'other package',
    #         'other application dram': 'other dram',
    #         'system package': 'jvm-c package',
    #         'system dram': 'jvm-c dram',
    #         'jvm package': 'jvm-java package',
    #         'jvm dram': 'jvm-java dram',
    #     })
    #     summary[benchmark]['benchmark'] = benchmark
    #
    #     runtime[benchmark]['benchmark'] = benchmark
    #     # runtime[benchmark]['mean'] = runtime[benchmark]['mean'][runtime[benchmark]['experiment'] == 'reference'].max()
    #
    # # energy summary
    # socket_summary = pd.concat(summary).groupby(['benchmark', 'socket']).sum().reset_index().sort_values('benchmark')
    #
    # ax = None
    # first = True
    # for socket, color, width in zip((1, 2), ('Reds', 'Blues'), (-0.125, 0.125)):
    #     soc = socket_summary[socket_summary['socket'] == socket].drop(columns = 'socket')
    #     print(soc)
    #     soc = soc.drop(columns = ['other package', 'other dram'])
    #     ax = soc.plot.bar(
    #         x = 'benchmark',
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
    # max_tick = np.ceil(socket_summary.drop(columns = ['other package', 'other dram']).drop(columns = ['benchmark']).sum(axis = 1).astype(int).max() / 100) * 100
    #
    # plt.xticks(fontsize = 8, rotation = 30)
    # plt.yticks(ticks = np.arange(0, max_tick, 100), fontsize = 8)
    # plt.xlabel('Benchmarks', fontsize = 12)
    # plt.ylabel('Energy (J)', fontsize = 12)
    #
    # plt.savefig(os.path.join(args.destination, 'attribution.svg'.format(suite)), bbox_inches = 'tight')
    #
    # # overhead
    # print(runtime)
    # runtime = pd.concat(runtime.values())
    # print(runtime)
    # # runtime = runtime[runtime['experiment'] != 'reference'].sort_values('benchmark')[['benchmark', 'mean', 'std', 'overhead', 'error']]
    # runtime = runtime.sort_values('benchmark')[['benchmark', 'mean', 'std', 'overhead', 'error']]
    # runtime.columns = ['benchmark', 'runtime', 'deviation', 'overhead', 'error']
    # runtime[['overhead', 'error']] *= 100
    # runtime['overhead'] = runtime['overhead'].map('{:.2f}%'.format)
    # runtime['error'] = runtime['error'].transform('{:.2f}%'.format)
    # runtime['runtime'] /= 10**3
    # runtime['runtime'] = runtime['runtime'].map('{:.2f} s'.format)
    # runtime['deviation'] /= 10**3
    # runtime['deviation'] = runtime['deviation'].map('{:.2f} s'.format)
    # runtime.columns = runtime.columns.str.title()
    # print(runtime)
    # runtime = runtime.to_latex(
    #     index = False,
    #     # float_format = '{:.2f}%'.format,
    #     column_format = '|l|r|r|r|r|'
    # )
    # runtime = '\documentclass{article}\n\\usepackage{booktabs}\n\\begin{document}\n' + runtime + '\end{document}'
    # pdf = build_pdf(runtime)
    # pdf.save_to(os.path.join(args.destination, 'overhead.pdf'))
    #
    # print('{:.2f} seconds for plotting'.format(time() - start))
    #
    # try:
    #     pass
    #     # plt.show()
    # except:
    #     print('No visual front end; skipping plt.show()')

if __name__=='__main__':
    main()
