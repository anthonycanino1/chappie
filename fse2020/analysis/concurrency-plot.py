#!/usr/bin/python3

# this needs plot testing

import json
import os
import os.path as op

from itertools import product

import matplotlib
matplotlib.use('Agg')

import matplotlib.pyplot as plt
import numpy as np
import pandas as pd

from tqdm import tqdm


def parse_timestamp(path):
    ts = np.sort([int(t) for t in json.load(open(path)).values()])
    return (np.max(ts) - np.min(ts)) / 1000000000

_RAPL_WRAPAROUND = 16384


def rapl_wrap_around(reading):
    if reading >= 0:
        return reading
    else:
        return max(reading + _RAPL_WRAPAROUND, 0)


def parse_energy(path, i):
    energy = pd.read_csv(path, delimiter = ';')

    energy.package = energy.groupby('socket').package.diff()
    energy.dram = energy.groupby('socket').dram.diff()

    energy.package = energy.package.map(rapl_wrap_around)
    energy.dram = energy.dram.map(rapl_wrap_around)
    energy = energy.fillna(0)

    energy = energy.groupby('epoch')[['package', 'dram']].sum().sum(axis = 1).reset_index()
    energy['timestamp'] = energy.epoch.map(i).fillna(0).astype(int) // 1000000
    energy.set_index('timestamp')[0]
    energy.name = 'energy'

    return energy


def filter_to_application_(trace):
    try:
        while len(trace) > 0:
            record = trace[0]
            exclude = False
            exclude = any((
                (r'.' not in record),
                (r'java.' in record and '.java\.' not in record),
                (r'javax.' in record and '.javax\.' not in record),
                (r'jdk.' in record and '.jdk\.' not in record),
                (r'sun.' in record and '.sun\.' not in record),
                (r'org.apache.commons.' in record and '.org.apache.commons\.' not in record),
                (r'<init>' in record),
                (r'.so' in record),
                (r'::' in record),
                (r'[' in record),
                (r']' in record)
            ))
            if not exclude:
                return trace
            else:
                trace.pop(0)
    except:
        pass

    return 'end'


def filter_to_application(df):
    mask = (df.trace == 'end') | df.trace.str.contains('chappie') | df.trace.str.contains('jlibc') | df.trace.str.contains('jrapl')
    df = df[~mask]
    df.trace = df.trace.str.split('@').map(filter_to_application_)
    method = df.trace.str[0]
    df = df[(df.trace != 'end') & (method != 'e') & ~(method.str.contains('org.dacapo.harness'))]

    return df


def ranking_plot(df):
    ax = df.plot.bar(
        legend = False,
        color = u'#2ca02c',
        edgecolor = 'black',
        figsize = (16, 9)
    )

    ax.spines['right'].set_visible(False)
    ax.spines['top'].set_visible(False)

    plt.ylim(0, ((5 * df.max().max()).astype(int) + 1) / 5)

    plt.xlabel('Method', fontsize = 24)
    plt.ylabel('Aware vs Oblivious Attributed Energy Ratio', fontsize = 24)

    plt.xticks(fontsize = 20, rotation = 45)
    plt.yticks(fontsize = 28)

    return ax.get_figure()


def main():
    if not op.exists('plots'):
        os.mkdir('plots')
    root = op.join('..', 'chappie-data', 'fse2020')

    ref_dir = op.join(root, 'freq')
    data_dir = op.join(root, 'profile')
    file_from = lambda k: op.join('raw', str(k))

    benchs = np.sort(os.listdir(ref_dir))
    benchs_ = tqdm(benchs)

    summary = []
    for bench in benchs_:
        benchs_.set_description(bench)

        if not op.exists('plots/{}'.format(bench)):
            os.mkdir('plots/{}'.format(bench))

        a = 2; b = 10

        df = pd.concat([pd.read_csv(
            op.join(data_dir, bench, str(n), file_from(k), 'method.csv'),
            delimiter = ';'
        ).assign(iter = k) for n, k in product(os.listdir(op.join(data_dir, bench)), range(a, b))])
        df.timestamp //= 1000000

        id = [{int(k): int(v) for k, v in json.load(open(
            op.join(data_dir, bench, str(n), file_from(k), 'time.json'))
        ).items()} for n, k in product(os.listdir(op.join(data_dir, bench)), range(a, b))]

        energy = pd.concat([parse_energy(
            op.join(data_dir, bench, str(n), file_from(k), 'energy.csv'),
            i
        ) for (n, k), i in zip(product(os.listdir(op.join(data_dir, bench)), range(a, b)), id)])

        df = pd.merge(df, energy, on = 'timestamp', how = 'left').dropna(subset = [0])
        df = filter_to_application(df)
        df['method'] = df.trace.str[0]
        obliv = df.groupby('method')[0].sum()
        obliv.name = 'energy'

        df = pd.concat([pd.read_csv(
            op.join(data_dir, bench, str(n), 'summary', 'method.csv')
        ) for n in os.listdir(op.join(data_dir, bench))])
        df['method'] = df.trace.str.split(';').str[0]
        df = df.groupby('method').energy.sum() * 8

        df = pd.concat([df, obliv], axis = 1).dropna()
        df.columns = ['Aware', 'Oblivious']
        df = df.sort_values(by = ['Aware'], ascending = False)
        df.index = df.index.str.split('.').str[-2:].str.join('.')

        summary.append(df.corr().iloc[0, 1])

        df = df.Oblivious / df.Aware

        ranking_plot(df.head(5))
        plt.savefig(op.join('plots', bench, 'concurrency-awareness.pdf'.format(bench)), bbox_inches = 'tight')
        plt.title(bench, fontsize = 32)
        plt.close()
    s = pd.Series(data = summary, index = benchs)
    s.name = 'correlation'
    s.index.name = 'bench'
    print(s)
    s.to_csv(op.join('plots', 'awareness-correlation.csv'), header = True)

if __name__ == '__main__':
    main()
