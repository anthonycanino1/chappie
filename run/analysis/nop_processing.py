#!/usr/bin/python3

import argparse
import json
import os
import subprocess

from csv import writer
from io import BytesIO, StringIO
from time import time
import xml.etree.ElementTree as ET

import numpy as np
import pandas as pd

def parse_args():
    parser = argparse.ArgumentParser()
    parser.add_argument('-benchmark')
    parser.add_argument('-config')

    return parser.parse_args()

def parse_config(benchmark, config):
    benchmark = json.load(open(benchmark, 'r'))

    root = ET.parse(config).getroot()
    for child in root:
        try:
            benchmark[child.tag] = int(child.text)
        except:
            benchmark[child.tag] = child.text

    return benchmark

if __name__ == '__main__':
    args = parse_args()
    config = parse_config(args.benchmark, args.config)

    args.path = config['workPath']

    # setup the paths
    if not os.path.exists(args.path):
        raise FileNotFoundError('No directory at {}'.format(os.path.abspath(args.path)))

    os.chdir(args.path)
    if not os.path.exists('processed'):
        os.mkdir('processed')

    files = np.sort([f for f in os.listdir() if 'runtime' in f])
    runtime = pd.concat([pd.read_csv(f) for f in files]).sort_values('name')

    runtime.to_csv(os.path.join('processed', 'chappie.runtime.csv'), index = False)
