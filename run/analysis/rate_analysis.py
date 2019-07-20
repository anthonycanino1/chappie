#!/usr/bin/python3

import argparse
import json
import os

from time import time
import xml.etree.ElementTree as ET

import matplotlib
matplotlib.use('Agg')

import matplotlib.pyplot as plt
import numpy as np
import pandas as pd
import seaborn as sns

from latex import build_pdf
from tabulate import tabulate

def parse_args():
    parser = argparse.ArgumentParser()
    parser.add_argument('-config')

    return parser.parse_args()

def parse_configs(config):
    configs = []
    for case in os.listdir(config):
        if 'NOP' not in case:
            configs.append({})
            root = ET.parse(os.path.join(args.config, case)).getroot()
            for child in root:
                try:
                    configs[-1][child.tag] = int(child.text)
                except:
                    configs[-1][child.tag] = child.text

    return configs

if __name__ == '__main__':
    args = parse_args()
    configs = parse_configs(args.config)

    path = os.path.dirname(args.config)

    miss_epoch = []
    miss_time = []
    active_epoch = []
    active_time = []
    for config in configs:
        print(config['workPath'].split(os.sep)[-1])
        print(pd.read_csv(os.path.join(config['workPath'], 'summary', 'chappie.epoch.csv')))
        continue
        tr = int(config['workPath'].split(os.sep)[-1].split('_')[0])

        df = pd.read_csv(os.path.join(config['workPath'], 'summary', 'chappie.frame.csv'))
        df.timestamp /= tr
        print(df.set_index('epoch').describe())
        continue

        df = pd.read_csv(os.path.join(config['workPath'], 'summary', 'chappie.miss.epoch.csv'))
        df.columns = ['epoch', config['workPath'].split(os.sep)[-1]]
        # miss_epoch.append(df.rolling(tr, win_type = 'hamming').mean().set_index('epoch').dropna())

        df1 = pd.read_csv(os.path.join(config['workPath'], 'summary', 'chappie.miss.timestamp.csv'))
        df1.columns = ['epoch', config['workPath'].split(os.sep)[-1]]

        df = pd.merge(df, df1, on = 'epoch', how = 'outer').fillna(0).set_index('epoch')
        # print(df.describe())
        # print(df.corr())

        # miss_time.append(df.rolling(tr, win_type = 'hamming').mean().set_index('timestamp').dropna())

        df = pd.read_csv(os.path.join(config['workPath'], 'summary', 'chappie.active.epoch.csv'))
        df.columns = ['epoch', config['workPath'].split(os.sep)[-1]]
        # print(df.count())
        active_epoch.append(df.set_index('epoch'))

        df1 = pd.read_csv(os.path.join(config['workPath'], 'summary', 'chappie.active.timestamp.csv'))
        df1.columns = ['epoch', config['workPath'].split(os.sep)[-1]]
        active_time.append(df.set_index('epoch'))

        # active_time.append(df.rolling(tr, win_type = 'hamming').mean().set_index('timestamp').dropna())

        df = pd.merge(df, df1, on = 'epoch', how = 'outer').fillna(0).set_index('epoch')
        # print(df.describe())
        # print(df)
        # print(df.corr())

    # print(pd.concat(active_time, axis = 1).describe())
    import sys
    sys.exit()

    miss_epoch = pd.concat(miss_epoch, axis = 1).fillna(0)
    miss_epoch.plot(logy = True)
    plt.title('epoch distance between skips')
    plt.savefig(os.path.join(path, 'missed_epoch.svg'), bbox_inches = 'tight')

    miss_time = pd.concat(miss_time, axis = 1).fillna(0)
    miss_time.plot(logy = True)
    plt.title('ms distance between skips')
    plt.savefig(os.path.join(path, 'missed_time.svg'), bbox_inches = 'tight')

    active_epoch = pd.concat(active_epoch, axis = 1).fillna(0)
    active_epoch.plot(logy = True)
    plt.title('epoch distance between records')
    plt.savefig(os.path.join(path, 'active_epoch.svg'), bbox_inches = 'tight')

    active_time = pd.concat(active_time, axis = 1).fillna(0)
    active_time.plot(logy = True)
    plt.title('ms distance between records')
    plt.savefig(os.path.join(path, 'active_time.svg'), bbox_inches = 'tight')
