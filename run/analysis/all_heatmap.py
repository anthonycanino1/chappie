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

def main():
    args = parse_args()

    # print(args.benchmark)
    path = args.benchmark
    if not os.path.exists(os.path.join(path, 'plots')):
        os.mkdir(os.path.join(path, 'plots'))

    benchs = []
    runtime = []
    for bench in os.listdir(path):
        if bench != 'plots':
            # df = pd.read_csv(os.path.join(path, bench, '1_{}'.format(bench), 'summary', 'chappie.runtime.csv'), index_col = 'experiment').dropna()
            df = pd.read_csv(os.path.join(path, bench, 'summary', 'chappie.runtime.csv'), index_col = 'experiment').dropna()
            if df.runtime.mean() >= 1.25:
                benchs.append(bench)
                df['tr'] = df.index.str.split('_').map(lambda x: x[0] if len(x) > 1 else x[0]).str.findall('\d+').map(lambda x: x[0]).astype(int)
                df['vm'] = df.index.str.split('_').map(lambda x: x[1] if len(x) > 1 else x[0]).str.findall('\d+').map(lambda x: x[0]).astype(int) * df['tr']
                df['os'] = df.index.str.split('_').map(lambda x: x[2] if len(x) > 1 else [0]).str.findall('\d+').map(lambda x: x[0] if x == x else '0').astype(int) * df['tr']

                runtime.append(df)

    runtime = pd.concat(runtime).groupby('experiment').mean()
    # print(runtime)

    runtime = runtime[runtime.os != 2]
    mu = runtime[runtime.os != 2].pivot(index = 'vm', columns = 'os', values = 'overhead').sort_index(ascending = False).round(2)
    # mu.columns = ['sleep'] + list(np.sort(mu.columns[1:]))

    sig = runtime[runtime.os != 2].pivot(index = 'vm', columns = 'os', values = 'overhead_std').sort_index(ascending = False).round(2)
    # sig.columns = ['sleep'] + list(np.sort(sig.columns[1:]))

    annot = mu.round(2).fillna('-').astype(str) + '±' + sig.round(2).astype(str)
    annot = annot.transform(lambda x: x.map(lambda y: (y + '%') if y != '-±nan' else '-'))
    # annot = mu.astype(str) + "±" + sig.astype(str)
    print(annot)

    ax = sns.heatmap(mu, cmap = 'Reds', annot = annot, fmt = '')
    ax.collections[0].colorbar.set_label('Percent Overhead')
    plt.xlabel("OS Sampling Rate (ms)")
    plt.ylabel("VM Sampling Rate (ms)")
    plt.savefig(os.path.join(path, 'plots', 'all_runtime_map.pdf'), bbox_inches = 'tight')

    plt.figure()

    mu = runtime[runtime.os != 2].pivot(index = 'vm', columns = 'os', values = 'rate').sort_index(ascending = False)
    # mu.columns = ['sleep'] + list(np.sort(mu.columns[1:]))

    # sig = runtime[runtime.os != 2].pivot(index = 'vm', columns = 'os', values = 'rate_std').sort_index(ascending = False).round(2)
    # sig.columns = ['sleep'] + list(np.sort(sig.columns[1:]))
    #
    # sig -= sig.sleep
    # mu -= mu.sleep

    annot = mu.round(2).fillna('-').astype(str).transform(lambda x: x.map(lambda y: (y + '%') if y != '-' else y))
    # .drop(columns = 'sleep')
    # sig = (sig * 100).round(2).drop(columns = 'sleep')

    # annot = mu.astype(str)
    # annot = mu.astype(str) + "+" + sig.astype(str)
    print(annot)

    # sns.heatmap(mu, cmap = 'Reds', annot = annot)
    ax = sns.heatmap(mu, cmap = 'Reds', annot = annot, fmt = '')
    ax.collections[0].colorbar.set_label('Rate Slowdown')
    plt.xlabel("OS Sampling Rate (ms)")
    plt.ylabel("VM Sampling Rate (ms)")
    plt.savefig(os.path.join(path, 'plots', 'all_rate_map.pdf'), bbox_inches = 'tight')

    plt.figure()

    methods = []
    for bench in benchs: # os.listdir(path):
        # if bench != 'plots':
        df = pd.read_csv(os.path.join(path, bench, 'summary', 'chappie.method.csv'), index_col = 'benchmark').dropna()
        methods.append(df)

                # df['tr'] = df.index.str.split('_').map(lambda x: x[0] if len(x) > 1 else x[0]).str.findall('\d+').map(lambda x: x[0]).astype(int)
                # df['vm'] = df.index.str.split('_').map(lambda x: x[1] if len(x) > 1 else x[0]).str.findall('\d+').map(lambda x: x[0]).astype(int) * df['tr']
                # df['os'] = df.index.str.split('_').map(lambda x: x[2] if len(x) > 1 else [0]).str.findall('\d+').map(lambda x: x[0] if x == x else '0').astype(int) * df['tr']


    methods = pd.concat(methods).groupby(['benchmark', 'stack']).mean().reset_index()
    methods = methods.pivot(index = 'stack', columns = 'benchmark', values = 'energy')
    methods = methods.corr()['timerRate1_vmFactor1_osFactor2'].to_frame(name = 'correlation')

    methods['tr'] = methods.index.str.split('_').map(lambda x: x[0] if len(x) > 1 else x[0]).str.findall('\d+').map(lambda x: x[0]).astype(int)
    methods['vm'] = methods.index.str.split('_').map(lambda x: x[1] if len(x) > 1 else x[0]).str.findall('\d+').map(lambda x: x[0]).astype(int) * methods['tr']
    methods['os'] = methods.index.str.split('_').map(lambda x: x[2] if len(x) > 1 else [0]).str.findall('\d+').map(lambda x: x[0] if x == x else '0').astype(int) * methods['tr']

    methods = methods[methods.os != 2]

    # methods = pd.read_csv(os.path.join(path, 'summary', 'chappie.method.csv')).dropna()
    # methods = methods.pivot(index = 'stack', columns = 'benchmark', values = 'energy')
    # methods = methods.corr().agg(('mean', 'std')).T
    # methods.columns = ['correlation', 'correlation_std']
    #
    # methods['timer'] = methods.index.str.split('_').map(lambda x: x[0]).str.findall('\d+').map(lambda x: x[0]).astype(int)
    # methods['os'] = methods.index.str.split('_').map(lambda x: x[1]).str.findall('\d+').map(lambda x: x[0]).astype(int)

    mu = methods.pivot(index = 'vm', columns = 'os', values = 'correlation').sort_index(ascending = False).round(2)
    # sig = (methods.pivot(index = 'vm', columns = 'os', values = 'correlation_std').sort_index(ascending = False) * 100).round(2)

    annot = mu.round(2).fillna('-').astype(str).fillna('-')
    # transform(lambda x: x.map(lambda y: (y + '%') if y != '-' else y))
    # annot = mu.astype(str) + "±" + sig.astype(str)
    print(annot)

    sns.heatmap(mu, cmap = 'Reds', annot = annot, fmt = '')
    ax = sns.heatmap(mu, cmap = 'Reds', annot = annot, fmt = '')
    ax.collections[0].colorbar.set_label('Percent Correlation')
    plt.xlabel("OS Sampling Rate (ms)")
    plt.ylabel("VM Sampling Rate (ms)")
    plt.savefig(os.path.join(path, 'plots', 'all_corr_map.pdf'), bbox_inches = 'tight')

if __name__ == '__main__':
    main()
