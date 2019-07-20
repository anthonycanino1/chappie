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

    bench = args.benchmark.split('/')[-1]
    path = os.path.join(args.benchmark)

    # path = path = os.path.join(
    #     os.path.dirname(os.path.dirname(args.benchmark)),
    #     os.path.splitext(os.path.basename(args.benchmark))[0]
    # )

    if not os.path.exists(os.path.join(args.benchmark, 'plots')):
        os.mkdir(os.path.join(args.benchmark, 'plots'))

    runtime = pd.read_csv(os.path.join(args.benchmark, 'summary', 'chappie.runtime.csv'), index_col = 'experiment').dropna()

    runtime['tr'] = runtime.index.str.split('_').map(lambda x: x[0] if len(x) > 1 else x[0]).str.findall('\d+').map(lambda x: x[0]).astype(int)
    runtime['vm'] = runtime.index.str.split('_').map(lambda x: x[1] if len(x) > 1 else x[0]).str.findall('\d+').map(lambda x: x[0]).astype(int) * runtime['tr']
    runtime['os'] = runtime.index.str.split('_').map(lambda x: x[2] if len(x) > 1 else [0]).str.findall('\d+').map(lambda x: x[0] if x == x else '0').astype(int) * runtime['tr']
    runtime = runtime[runtime.os != 2]

    mu = runtime.pivot(index = 'vm', columns = 'os', values = 'overhead').sort_index(ascending = False).round(2)
    sig = runtime.pivot(index = 'vm', columns = 'os', values = 'overhead_std').sort_index(ascending = False).round(2)

    annot = mu.round(2).fillna('-').astype(str) + '±' + sig.round(2).astype(str)
    annot = annot.transform(lambda x: x.map(lambda y: (y + '%') if y != '-±nan' else '-'))

    print(annot)

    ax = sns.heatmap(mu, cmap = 'Reds', annot = annot, fmt = '')
    ax.collections[0].colorbar.set_label('Percent Overhead')
    plt.xlabel("OS Sampling Rate (ms)")
    plt.ylabel("VM Sampling Rate (ms)")
    plt.savefig(os.path.join(args.benchmark, 'plots', 'runtime_map.pdf'), bbox_inches = 'tight')

    plt.figure()

    # print(runtime)
    mu = runtime.pivot(index = 'vm', columns = 'os', values = 'rate').sort_index(ascending = False)
    # print(mu)
    # mu.columns = ['sleep'] + list(np.sort(mu.columns[1:]))

    # sig = runtime.pivot(index = 'vm', columns = 'os', values = 'overhead_std').sort_index(ascending = False).round(2)
    # sig.columns = ['sleep'] + list(np.sort(sig.columns[1:]))

    # sig -= sig.sleep
    # mu -= mu.sleep

    # mu = (mu * 100).round(2).drop(columns = 'sleep')
    # sig = (sig * 100).round(4).drop(columns = 'sleep')

    # mu = runtime.pivot(index = 'vm', columns = 'os', values = 'rate').sort_index(ascending = False).round(2)
    # mu.columns = ['sleep'] + list(np.sort(mu.columns[1:]))
    # sig = runtime.pivot(index = 'vm', columns = 'os', values = 'rate_std').sort_index(ascending = False).round(2)
    # sig.columns = ['sleep'] + list(np.sort(sig.columns[1:]))
    annot = mu.round(2).fillna('-').astype(str).transform(lambda x: x.map(lambda y: (y + '%') if y != '-' else y))
    # annot = mu.astype(str) + "+" + sig.astype(str)
    print(annot)

    # sns.heatmap(mu, cmap = 'Reds', annot = annot)
    ax = sns.heatmap(mu, cmap = 'Reds', annot = annot, fmt = '')
    ax.collections[0].colorbar.set_label('Rate Slowdown')
    plt.xlabel("OS Sampling Rate (ms)")
    plt.ylabel("VM Sampling Rate (ms)")
    plt.savefig(os.path.join(args.benchmark, 'plots', 'rate_map.pdf'), bbox_inches = 'tight')

    plt.figure()

    methods = pd.read_csv(os.path.join(path, 'summary', 'chappie.method.csv')).dropna()
    methods = methods.pivot(index = 'stack', columns = 'benchmark', values = 'energy')
    methods = methods.corr()['timerRate1_vmFactor1_osFactor2'].to_frame(name = 'correlation')
    # methods = methods.corr().agg(('mean', 'std')).T
    # methods.columns = ['correlation', 'correlation_std']

    methods['tr'] = methods.index.str.split('_').map(lambda x: x[0] if len(x) > 1 else x[0]).str.findall('\d+').map(lambda x: x[0]).astype(int)
    methods['vm'] = methods.index.str.split('_').map(lambda x: x[1] if len(x) > 1 else x[0]).str.findall('\d+').map(lambda x: x[0]).astype(int) * methods['tr']
    methods['os'] = methods.index.str.split('_').map(lambda x: x[2] if len(x) > 1 else [0]).str.findall('\d+').map(lambda x: x[0] if x == x else '0').astype(int) * methods['tr']
    methods = methods[methods.os != 2]

    mu = methods.pivot(index = 'vm', columns = 'os', values = 'correlation').sort_index(ascending = False).round(2)
    # sig = (methods.pivot(index = 'vm', columns = 'os', values = 'correlation_std').sort_index(ascending = False) * 100).round(2)

    annot = mu.round(2).fillna('-').astype(str).fillna('-')

    # mu = (methods.pivot(index = 'vm', columns = 'os', values = 'correlation').sort_index(ascending = False) * 100).round(2)
    # # sig = (methods.pivot(index = 'vm', columns = 'os', values = 'correlation_std').sort_index(ascending = False) * 100).round(2)
    #
    # annot = mu.round(2).fillna('-').astype(str).transform(lambda x: x.map(lambda y: (y + '%') if y != '-' else y))
    # annot = mu.astype(str) + "±" + sig.astype(str)
    print(annot)

    ax = sns.heatmap(mu, cmap = 'Reds_r', annot = annot, fmt = '')
    ax.collections[0].colorbar.set_label('Percent Correlation')
    plt.xlabel("OS Sampling Rate (ms)")
    plt.ylabel("VM Sampling Rate (ms)")
    plt.savefig(os.path.join(args.benchmark, 'plots', 'corr_map.pdf'), bbox_inches = 'tight')

if __name__ == '__main__':
    print('?')
    main()
