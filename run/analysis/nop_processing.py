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
    parser.add_argument('-path', default = "chappie_test")
    parser.add_argument('-destination', default = None)
    args = parser.parse_args()

    # setup the paths
    if not os.path.exists(args.path):
        raise FileNotFoundError('No directory at {}'.format(os.path.abspath(args.path)))

    if args.destination is None:
        args.destination = os.path.join(args.path, 'processed')

    if not os.path.exists(args.destination):
        os.mkdir(args.destination)

    runtime = np.sort([os.path.join(args.path, f) for f in os.listdir(args.path) if 'runtime' in f])

    for f in runtime:
        runtime = pd.read_csv(f)

        # grab the runtime value
        application_runtime = runtime[runtime['name'] == 'runtime']['value']
        application_runtime.to_csv(os.path.join(args.destination, 'chappie.runtime.csv'), mode = 'a', index = False, header = False)
