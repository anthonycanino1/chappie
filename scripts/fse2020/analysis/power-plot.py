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

def parse_timestamp(path):
    ts = np.sort([int(t) for t in json.load(open(path)).values()])
    return (np.max(ts) - np.min(ts)) / 1000000000

_RAPL_WRAPAROUND = 16384

def rapl_wrap_around(reading):
    if reading >= 0:
        return reading
    else:
        return max(reading + _RAPL_WRAPAROUND, 0)

def parse_energy(path):
    energy = pd.read_csv(path, delimiter = ';')

    energy.package = energy.groupby('socket').package.diff()
    energy.dram = energy.groupby('socket').dram.diff()

    energy.package = energy.package.map(rapl_wrap_around)
    energy.dram = energy.dram.map(rapl_wrap_around)
    energy = energy.fillna(0)

    return energy[['package', 'dram']].sum().sum()

def main():
    if not os.path.exists('plots'):
        os.mkdir('plots')
    root = os.path.join('..', 'chappie-data', 'fse2020')

    ref_dir = os.path.join(root, 'freq')
    data_dir = os.path.join(root, 'calmness')
    file_from = lambda k: os.path.join('raw', str(k))

    benchs = np.sort(os.listdir(ref_dir))
    # benchs = ['h2', 'batik']
    benchs = tqdm(benchs)

    summary = []

    for bench in benchs:
        benchs.set_description(bench + " - ref")

        if not os.path.exists('plots/{}'.format(bench)):
            os.mkdir('plots/{}'.format(bench))

        if bench in ('fop', 'jme', 'kafka'):
            a = 20
            b = 100
        else:
            a = 2
            b = 10

        e = [parse_energy(
            os.path.join(ref_dir, bench, file_from(str(k)), 'energy.csv')
        ) for k in range(a, b)]

        t = [parse_timestamp(
            os.path.join(ref_dir, bench, file_from(str(k)), 'time.json')
        ) for k in range(a, b)]

        ref = {
            'e_m': np.mean(e),
            't_m': np.mean(t),
            'e_s': np.std(e),
            't_s': np.std(t),
        }
        ref['p_m'] = ref['e_m'] / ref['t_m']
        ref['p_s'] = np.sqrt((ref['e_s'] / ref['e_m'])**2 + (ref['t_s'] / ref['t_m'])**2)

        stats = []
        for rate in os.listdir(os.path.join(data_dir, bench)):
            benchs.set_description(bench + " - " + rate)

            e = [parse_energy(
                os.path.join(data_dir, bench, rate, file_from(str(k)), 'energy.csv')
            ) for k in range(a, b)]

            t = [parse_timestamp(
                os.path.join(data_dir, bench, rate, file_from(str(k)), 'time.json')
            ) for k in range(a, b)]

            d = {
                'e_m': np.mean(e) / ref['e_m'],
                't_m': np.mean(t) / ref['t_m'],
                'e_s': np.sqrt((np.std(e) / np.mean(e))**2 + (ref['e_s'] / ref['e_m'])**2),
                't_s': np.sqrt((np.std(t) / np.mean(t))**2 + (ref['t_s'] / ref['t_m'])**2),
            }

            d['p_m'] = d['e_m'] / d['t_m']
            d['p_s'] = np.sqrt((d['e_s'] / d['e_m'])**2 + (d['t_s'] / d['t_m'])**2)

            d['e_m'] -= 1
            d['t_m'] -= 1
            d['p_m'] -= 1

            d['rate'] = int(rate)

            stats.append(pd.Series(d))
            benchs.set_description(bench)

        df = pd.concat(stats, axis = 1).T.set_index('rate').sort_index()[['e_m', 't_m', 'p_m', 'e_s', 't_s', 'p_s']] * 100
        df.index = df.index.astype(int)
        df.columns = pd.MultiIndex.from_tuples(product(('mean', 'std'), ('energy', 'time', 'power')))
        df.to_csv('plots/{}/calmness.csv'.format(bench))

        summary.append(df.assign(bench = bench))
        continue
        print(df)
        df = df[df.index <= 64]
        print(df)

        ax = df.plot.bar(
            y = 'mean',
            yerr = 'std',
            error_kw = dict(lw = 2, capsize = 8, capthick = 1),
            width = 0.85,
            edgecolor = 'black',
            color = [u'#2ca02c', u'#d62728', u'#1f77b4'],
            figsize = (10, 5.5),
        )

        plt.title(bench, fontsize = 32)
        ax.legend(labels = ['energy', 'time', 'power'], loc = 'upper right', fontsize = 20)

        plt.xlabel('Sampling Rate (ms)', fontsize = 20)
        plt.ylabel('Normalized Energy/Time/Power', fontsize = 20)

        plt.xticks(fontsize = 24, rotation = 30)
        plt.yticks(fontsize = 24)

        plt.hlines(0, -1, len(df.index), color = 'black', linestyles = 'dashed', alpha = 1)

        plt.savefig('plots/{}/power-stability.pdf'.format(bench), bbox_inches = 'tight')
        plt.close()

    summary = pd.concat(summary)
    summary = summary[summary.index == summary.bench.map(rates)]
    print(summary.mean())

if __name__ == '__main__':
    main()
