#!/usr/bin/python3

import json
import os

from itertools import product

import matplotlib
matplotlib.use('Agg')

import matplotlib.patheffects as path_effects
import matplotlib.pyplot as plt
import numpy as np
import pandas as pd
import seaborn as sns

from scipy.spatial.distance import *
from tqdm import tqdm

rates = {
    'avrora': 128,
    'batik': 8,
    'biojava': 8,
    'eclipse': 16,
    'fop10': 4,
    'graphchi': 16,
    'h2': 32,
    'jme10': 128,
    'jython': 8,
    'pmd': 16,
    'sunflow': 16,
    'tomcat': 16,
    'xalan': 16
}

def magnitude(x):
    return int(np.log10(x))

def bin_count(s, n):
    iqr = 2 * (s.quantile(0.75) - s.quantile(0.25))
    if iqr > 0:
        d = s.max() - s.min()
        size = int(d * np.cbrt(n) / iqr)
    else:
        size = int(np.ceil(np.log2(n))) + 1

    if size > n:
        size = int(np.ceil(np.log2(n))) + 1

    # if size < 3:
    #     size = 3
    # if size % 2 == 0:
    #     size += 1

    return size

def main():
    if not os.path.exists('plots'):
        os.mkdir('plots')
    root = os.path.join('..', 'chappie-data', 'fse2020')

    ref_dir = os.path.join(root, 'freq')
    data_dir = os.path.join(root, 'calmness')
    freq_file = lambda k: os.path.join('raw', str(k), 'freqs.csv')

    benchs = np.sort(os.listdir(ref_dir))
    # benchs = ['batik']
    benchs = tqdm(benchs)

    summary = []

    for bench in benchs:
        benchs.set_description(bench + " - ref")

        if not os.path.exists('plots/{}'.format(bench)):
            os.mkdir('plots/{}'.format(bench))

        if bench == 'fop10':
            continue

        if bench in ('fop10', 'jme10', 'kafka10'):
            a = 20
            b = 100
        else:
            a = 2
            b = 10

        df = pd.concat([pd.read_csv(
            os.path.join(ref_dir, bench, freq_file(k)),
            delimiter = ';'
        ).assign(iter = k) for k in range(a, b)])
        df.freq /= 10000
        df.freq = df.freq.astype(int)
        end = df.epoch.max()

        size = bin_count(df.freq, 40)
        if size > 1:
            freq_bins = np.linspace(df.freq.min(), df.freq.max(), size + 1)
            df['freq'] = pd.cut(df.freq, bins = freq_bins)
            df = df.groupby(['epoch', 'freq', 'iter']).cpu.count().to_frame().reset_index()
        else:
            freq_bins = None
            df = df.groupby(['epoch', 'iter']).cpu.count().to_frame().reset_index()

        size = bin_count(df.cpu, 8)
        if size > 1:
            cpu_bins = np.linspace(0, 40, size)
            df.cpu = pd.cut(df.cpu, bins = cpu_bins)
            df = df.groupby(['epoch', 'freq', 'cpu']).iter.count()
        else:
            cpu_bins = None
            df = df.groupby(['epoch', 'freq']).iter.count()

        if freq_bins is None and cpu_bins is None:
            idx = pd.Index(range(1, end + 1))
        elif freq_bins is None:
            idx = pd.MultiIndex.from_product((range(1, end + 1), cpu_bins))
        elif cpu_bins is None:
            idx = pd.MultiIndex.from_product((range(1, end + 1), freq_bins))
        else:
            idx = pd.MultiIndex.from_product((range(1, end + 1), freq_bins, cpu_bins))
        df = df.reindex(idx).fillna(0).astype(int)

        ref = df

        dists = []
        for rate in os.listdir(os.path.join(data_dir, bench)):
            benchs.set_description(bench + " - " + rate)

            df = pd.concat([pd.read_csv(
                os.path.join(data_dir, bench, rate, freq_file(k)),
                delimiter = ';'
            ).assign(iter = k) for k in range(a, b)])

            df.epoch //= df.epoch.min()

            df.freq /= 10000
            df.freq = df.freq.astype(int)

            if freq_bins is not None:
                df['freq'] = pd.cut(df.freq, bins = freq_bins)
                df = df.groupby(['epoch', 'freq', 'iter']).cpu.count().to_frame().reset_index()
            else:
                df = df.groupby(['epoch', 'iter']).cpu.count().to_frame().reset_index()

            if cpu_bins is not None:
                df.cpu = pd.cut(df.cpu, bins = cpu_bins)
                df = df.groupby(['epoch', 'freq', 'cpu']).iter.count()
            else:
                df = df.groupby(['epoch', 'freq']).iter.count()

            df = df.reindex(idx).fillna(0).astype(int)

            dists.append((int(rate), df.corr(ref), np.sqrt((1 - df.corr(ref)**2) / len(df))))

        df = pd.DataFrame(data = dists, columns = ['rate', 'correlation', 'err']).set_index('rate').sort_index()

        summary.append(df.reset_index().assign(bench = bench))
    df = pd.concat(summary).set_index(['rate', 'bench']).correlation
    df = df.unstack()
    df.to_csv('plots/temporal-correlation.csv')
    print(df)

if __name__ == '__main__':
    main()
