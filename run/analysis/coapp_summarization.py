#!/usr/bin/python3

import argparse
import os

from time import time

import numpy as np
import pandas as pd

if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('-path', default = "chappie.chappie_test")
    parser.add_argument('-destination', default = None)
    args = parser.parse_args()

    # setup the paths
    if not os.path.exists(args.path):
        raise FileNotFoundError('No directory at {}'.format(os.path.abspath(args.path)))

    if args.destination is None:
        args.destination = os.path.join(args.path, 'summary')

    if not os.path.exists(args.destination):
        os.mkdir(args.destination)

    summary = []
    correlation = []
    runtime = []
    for f in os.listdir(args.path):
        if f != 'summary':
            df = pd.read_csv(os.path.join(args.path, f, 'summary', 'chappie.component.csv')).drop(columns = ['other application package', 'other application dram'])
            print(f)
            df['order'] = int(f.split('_')[-1])
            summary.append(df.drop(columns = ['total package', 'total dram']))

            df = pd.read_csv(os.path.join(args.path, f, 'summary', 'chappie.correlation.csv'))
            df = df.groupby(['level', 'type', 'value']).sum()
            df['order'] = int(f.split('_')[-1])
            correlation.append(df)

            df = pd.read_csv(os.path.join(args.path, f, 'summary', 'chappie.runtime.csv'))
            df['order'] = int(f.split('_')[-1])
            runtime.append(df)

    summary = pd.concat(summary)
    summary.to_csv(os.path.join(args.path, 'summary', 'chappie.component.csv'), index = False)

    correlation = pd.concat(correlation)
    correlation.to_csv(os.path.join(args.path, 'summary', 'chappie.correlation.csv'))

    runtime = pd.concat(runtime)
    runtime.to_csv(os.path.join(args.path, 'summary', 'chappie.runtime.csv'))
