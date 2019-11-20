import argparse
import json
import os

from argparse import Namespace
from os.path import dirname

import pandas as pd

from numpy.random import randint
from tqdm import tqdm

import attribution as attr
import summary as smry
import plotting as plt

run_libs = dirname(__file__)
chappie_root = dirname(run_libs)

def parse_args():
    parser = argparse.ArgumentParser()

    parser.add_argument('-dir', '--work-directory')

    args = parser.parse_args()

    if os.path.exists(os.path.join(args.work_directory, 'config.json')):
        config = json.load(open(os.path.join(args.work_directory, 'config.json')))
        config['work_directory'] = args.work_directory
    else:
        raise ValueError('no config found!')

    return config

def processing(work_directory):
    raw_root = os.path.join(work_directory, 'raw')
    processed_root = os.path.join(work_directory, 'processed')
    if not os.path.exists(processed_root):
        os.mkdir(processed_root)

    iters = [f for f in os.listdir(raw_root) if 'method' not in f]
    iters.sort()
    warm_up = len(iters) // 5
    iters = iters[warm_up:]

    if os.path.exists(os.path.join(raw_root, 'method.csv')):
        raw_method = pd.read_csv(os.path.join(raw_root, 'method.csv'))
        raw_method.columns = ['name', 'timestamp', 'id', 'trace']
        raw_method = raw_method[raw_method.trace != 'end'].drop_duplicates(subset = ['timestamp', 'id'])
    else:
        raw_method = None

    status = tqdm(iters)

    for f in status:
        raw_path = os.path.join(raw_root, f)
        if not os.path.exists(os.path.join(processed_root, 'energy')):
            os.mkdir(os.path.join(processed_root, 'energy'))

        if raw_method is None:
            df = pd.read_csv(os.path.join(raw_path, 'energy.csv'), delimiter = ';')
            df['energy'] = df.package + df.dram
            df = df.groupby('epoch')[['energy']].sum()
            timestamps = {int(k) + 1: int(v) for k, v in json.load(open(os.path.join(raw_path, 'time.json'))).items()}
            df['timestamp'] = df.index.map(timestamps)
            df = df.diff().fillna(0)
            df.to_csv(os.path.join(processed_root, 'energy', '{}.csv'.format(f)), index = False)
            continue

        if not os.path.exists(os.path.join(processed_root, 'method')):
            os.mkdir(os.path.join(processed_root, 'method'))

        energy = attr.attribute(raw_path, status)
        energy.to_csv(os.path.join(processed_root, 'energy', '{}.csv'.format(f)))

        # energy = pd.read_csv(os.path.join(processed_root, 'energy', '{}.csv'.format(f))).set_index(['timestamp', 'id', 'name'])

        energy = energy.reset_index()
        energy = energy[energy.id > 0].set_index(['timestamp', 'id'])

        timestamps = json.load(open(os.path.join(raw_root, f, 'time.json')))
        timestamps = {int(k): int(v) for k, v in timestamps.items()}
        start, end = min(timestamps.values()), max(timestamps.values())

        id = json.load(open(os.path.join(raw_path, 'id.json')))

        if raw_method is not None:
            trimmed_method = raw_method[(raw_method.timestamp >= start) & (raw_method.timestamp <= end)].copy()
            trimmed_method['timestamp'] = trimmed_method.timestamp - start + 1
            trimmed_method = trimmed_method.set_index(['timestamp', 'id'])
        else:
            trimmed_method = pd.DataFrame(index = energy.index)
            trimmed_method['trace'] = randint(0, 26, len(trimmed_method)) + 65
            trimmed_method.trace = trimmed_method.trace.map(chr)

        # if os.path.exists(os.path.join(raw_path, 'method.csv')):
        #     method = pd.read_csv(os.path.join(raw_path, 'method.csv'), delimiter = ';')
        #     method.timestamp = method.timestamp - start + 1
        #     method = method.set_index(['timestamp', 'id']).sort_index()

        df = attr.align(energy, trimmed_method, id, limit = 0, status = status)
        df.to_csv(os.path.join(processed_root, 'method', '{}.csv'.format(f)))

def summary(work_directory):
    processed_root = os.path.join(work_directory, 'processed')
    summary_root = os.path.join(work_directory, 'summary')
    if not os.path.exists(summary_root):
        os.mkdir(summary_root)

    runtime = smry.runtime(work_directory)
    runtime.to_csv(os.path.join(summary_root, 'runtime.csv'), header = True)
    print(runtime)
    #
    # return

    if os.path.exists(os.path.join(work_directory, 'raw', 'method.csv')):
        # component = smry.component(os.path.join(processed_root, 'energy'))
        # component.to_csv(os.path.join(summary_root, 'component.csv'))
        # print(component)

        method = smry.method(os.path.join(processed_root, 'method'))
        method.to_csv(os.path.join(summary_root, 'method.csv'))

def plotting(work_directory):
    if not os.path.exists(os.path.join(work_directory, 'raw', 'method.csv')):
        return

    summary_root = os.path.join(work_directory, 'summary')
    plots_root = os.path.join(work_directory, 'plots')
    if not os.path.exists(plots_root):
        os.mkdir(plots_root)
    else:
        import shutil
        shutil.rmtree(plots_root)
        os.mkdir(plots_root)

    correlation = plt.correlation(summary_root)
    print(correlation.head(20))

    ranking = plt.ranking(summary_root)
    print(ranking.head(20))

    cfa = plt.cfa(summary_root)
    cfa.to_csv(os.path.join(summary_root, 'cfa2.csv'))
    print(cfa)

def main(config):
    processing(config['work_directory'])
    summary(config['work_directory'])
    plotting(config['work_directory'])

if __name__ == "__main__":
    main(parse_args())
