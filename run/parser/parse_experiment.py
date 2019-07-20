#!/usr/bin/python3

import argparse
import json
import os
import xml.etree.ElementTree as ET

from collections import OrderedDict
from itertools import product

import dicttoxml

CHAPPIE_PATH = os.path.dirname(os.path.dirname(__file__))

def parse_args():
    parser = argparse.ArgumentParser()
    parser.add_argument('-experiment')

    return parser.parse_args()

def main():
    args = parse_args()
    experiment = json.load(open(args.experiment, 'r'), object_pairs_hook = OrderedDict)

    nop = ET.parse(os.path.join(CHAPPIE_PATH, 'config', 'NOP.xml'))
    nop_root = nop.getroot()

    sample = ET.parse(os.path.join(CHAPPIE_PATH, 'config', 'SAMPLE.xml'))
    sample_root = sample.getroot()

    chappie_params = experiment['chappie']

    root = experiment['workDir']
    path = None
    for d in root.split(os.sep):
        if path is None:
            path = d
        else:
            path = os.path.join(path, d)
        if not os.path.exists(path):
            os.mkdir(path)
    if not os.path.exists(os.path.join(path, 'config')):
        os.mkdir(os.path.join(path, 'config'))
    if not os.path.exists(os.path.join(path, 'benchmark')):
        os.mkdir(os.path.join(path, 'benchmark'))

    for case in experiment['cases']:
        benchmark = json.load(open(os.path.join(CHAPPIE_PATH, 'benchmark', '{}.json'.format(case['config'])), 'r'))
        benchmark.update(case)
        json.dump(benchmark, open(os.path.join(path, 'benchmark', 'benchmark.json'.format()), 'w'))

        nop_root.find('workPath').text = os.path.join(path, 'reference')
        nop.write(os.path.join(path, 'config', 'NOP.xml'))

        for params in product(*chappie_params.values()):
            name = '_'.join(str(p) for p in params)
            sample_root.find('workPath').text = os.path.join(path, name)

            for key, value in zip(chappie_params, params):
                sample_root.find(key).text = str(value)

            sample.write(os.path.join(path, 'config', name + '.xml'))

    print(path)

if __name__ == '__main__':
    main()
