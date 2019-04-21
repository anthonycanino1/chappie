#!/usr/bin/python3

import argparse
import os

import numpy as np
import pandas as pd

def df_diff(df, by, values):
    return pd.concat([
        df.rename(columns = {value: value + '_' for value in values}),
        df.groupby(by)[values].diff().fillna(0)
    ], axis = 1).drop(columns = [value + '_' for value in values])

if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('-path', default = "chappie_test")
    parser.add_argument('-reference', default = "chappie_test")
    parser.add_argument('-destination', default = None)
    args = parser.parse_args()

    # setup the paths
    if not os.path.exists(args.path):
        raise FileNotFoundError('No directory at {}'.format(os.path.abspath(args.path)))

    print(args.reference)
    if not os.path.exists(args.reference):
        raise FileNotFoundError('No directory at {}'.format(os.path.abspath(args.reference)))

    args.path = os.path.join(args.path, 'summary')
    if args.destination is None:
        args.destination = args.path
    args.reference = os.path.join(args.reference, 'summary')

    method = pd.read_csv(os.path.join(args.path, 'chappie.method.csv'))
    ref = pd.read_csv(os.path.join(args.reference, 'chappie.method.csv'))

    method = pd.merge(method, ref, on = ['level', 'name', 'type'], suffixes = ('', ' Reference')).fillna(0)

    energy = method.groupby(['level', 'type'])[['Energy', 'Energy Reference']].corr()
    energy = energy.reset_index().drop_duplicates(['level', 'type'])
    energy.columns = ['level', 'type', 'value', '', 'Correlation']

    # time_ = method.groupby(['level', 'type'])[['Time', 'Time Reference']].corr()
    # time_ = time_.reset_index().drop_duplicates(['level', 'type'])
    # time_.columns = ['level', 'type', 'value', '', 'Correlation']

    # corr = pd.concat([energy, time_]).drop(columns = '')
    corr = energy.drop(columns = '')
    print(corr)

    corr.to_csv(os.path.join(args.destination, 'chappie.correlation.csv'), index = False)
