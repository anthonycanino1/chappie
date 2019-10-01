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

run_libs = dirname(__file__)
chappie_root = dirname(run_libs)

def parse_args():
    parser = argparse.ArgumentParser()

    parser.add_argument('-cfg', '--config')
    parser.add_argument('-dir', '--work-directory', default = 'chappie-logs')

    args = parser.parse_args()

    if os.path.exists(os.path.join(args.work_directory, 'config.json')):
        config = json.load(open(os.path.join(args.work_directory, 'config.json')))
    elif args.config:
        config = json.load(open(args.config))
    else:
        raise ArgumentError('no config found!')

    return config

def processing(work_directory):
    raw_root = os.path.join(work_directory, 'raw')
    processed_root = os.path.join(work_directory, 'processed')
    if not os.path.exists(processed_root):
        os.mkdir(processed_root)

    iters = [f for f in os.listdir(raw_root) if 'method' not in f]

    try:
        raw_method = pd.read_csv(os.path.join(raw_root, 'method.csv'), header = None)
        raw_method.columns = ['name', 'timestamp', 'id', 'trace']
        raw_method = raw_method[raw_method.trace != 'end'].drop_duplicates(subset = ['timestamp', 'id'])
    except:
        raw_method = None

    status = tqdm(iters)
    for f in status:
        raw_path = os.path.join(raw_root, f)
        if not os.path.exists(os.path.join(processed_root, 'energy')):
            os.mkdir(os.path.join(processed_root, 'energy'))
        if not os.path.exists(os.path.join(processed_root, 'method')):
            os.mkdir(os.path.join(processed_root, 'method'))

        energy = attr.attribute(raw_path, status)
        energy.to_csv(os.path.join(processed_root, 'energy', '{}.csv'.format(f)))

        energy = energy.reset_index()
        energy = energy[energy.id > 0].set_index(['timestamp', 'id'])

        timestamps = json.load(open(os.path.join(raw_root, f, 'time.json')))
        timestamps = {int(k): int(v) for k, v in timestamps.items()}
        start, end = min(timestamps.values()), max(timestamps.values())

        if raw_method is not None:
            trimmed_method = raw_method[(raw_method.timestamp >= start) & (raw_method.timestamp <= end)].copy()
            trimmed_method['timestamp'] = trimmed_method.timestamp - start + 1
            trimmed_method = trimmed_method.set_index(['timestamp', 'id'])
        else:
            trimmed_method = pd.DataFrame(index = energy.index)
            trimmed_method['trace'] = randint(0, 26, len(trimmed_method)) + 65
            trimmed_method.trace = trimmed_method.trace.map(chr)

        id = json.load(open(os.path.join(raw_root, f, 'id.json')))
        method = attr.align(energy, trimmed_method, id, limit = None, status = status)
        method.to_csv(os.path.join(processed_root, 'method', '{}.csv'.format(f)))

def summary(work_directory):
    processed_root = os.path.join(work_directory, 'processed')
    summary_root = os.path.join(work_directory, 'summary')
    if not os.path.exists(summary_root):
        os.mkdir(summary_root)

    # component = smry.component(os.path.join(processed_root, 'energy'))
    # component = component.sort_index()
    # print(component.sort_index())
    # component.to_csv(os.path.join(summary_root, 'component.csv'))

    method = smry.method(os.path.join(processed_root, 'method'))
    method = method.sort_values(['energy', 'hits'], ascending = False)
    print(method)
    method.to_csv(os.path.join(summary_root, 'method.csv'))

def main(config):
    # processing(config['work_directory'])
    summary(config['work_directory'])
    # plot(config.work_directory)

if __name__ == "__main__":
    main(parse_args())
