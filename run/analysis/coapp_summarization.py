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
    for f in os.listdir(args.path):
        if f != 'summary':
            df = pd.read_csv(os.path.join(args.path, f, 'summary', 'chappie.component.csv')).drop(columns = ['other application package', 'other application dram'])
            df.columns = ['socket'] + [f + ' ' + col for col in df.columns][1:]
            summary.append(df)

    summary = pd.merge(*summary, on = 'socket')
    summary['total package'] = summary[[col for col in summary.columns if 'total package' in col]].sum(axis = 1) / (len(os.listdir(args.path)) - 1)
    summary['total dram'] = summary[[col for col in summary.columns if 'total dram' in col]].sum(axis = 1) / (len(os.listdir(args.path)) - 1)
    summary = summary.drop(columns = [col for col in summary.columns if 'total package' in col and 'total package' != col])
    summary = summary.drop(columns = [col for col in summary.columns if 'total dram' in col and 'total dram' != col])
    summary['other application package'] = summary['total package'] - summary[[col for col in summary.columns if 'total' not in col and 'package' in col]].sum(axis = 1)
    summary['other application dram'] = summary['total dram'] - summary[[col for col in summary.columns if 'total' not in col and 'dram' in col]].sum(axis = 1)

    summary = summary.reindex(['socket'] + sorted([col for col in summary.columns if col != 'socket'], reverse = True), axis = 1)
    summary.to_csv(os.path.join(args.path, 'summary', 'chappie.component.csv'), index = False)
