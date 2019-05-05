#!/usr/bin/python3

import argparse
import os
import subprocess

from csv import writer
from io import BytesIO, StringIO
from time import time

import numpy as np
import pandas as pd

if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('-path')

    args = parser.parse_args()

    # setup the paths
    if not os.path.exists(args.path):
        raise FileNotFoundError('No directory at {}'.format(os.path.abspath(args.path)))

    os.chdir(args.path)
    if not os.path.exists('processed'):
        os.mkdir('processed')

    files = np.sort([f for f in os.listdir() if 'runtime' in f])
    runtime = pd.concat([pd.read_csv(f) for f in files])

    runtime.to_csv(os.path.join('processed', 'chappie.runtime.csv'), index = False)
