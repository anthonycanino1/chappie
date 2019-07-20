#!/usr/bin/python3

import argparse
import json
import os

import xml.etree.ElementTree as ET

from collections import OrderedDict
from itertools import product

import numpy as np

CHAPPIE_PATH = os.path.dirname(os.path.dirname(__file__))

def parse_args():
    parser = argparse.ArgumentParser()
    parser.add_argument('-experiment')

    return parser.parse_args()

def main():
    args = parse_args()
    experiment = json.load(open(args.experiment, 'r'), object_pairs_hook = OrderedDict)

    work_dir = experiment['work_dir']
    if not os.path.exists(work_dir):
        os.makedirs(work_dir)

    config_dir = os.path.join(work_dir, 'config')
    if not os.path.exists(config_dir):
        os.mkdir(config_dir)

    benchmark_dir = os.path.join(work_dir, 'benchmark')
    if not os.path.exists(benchmark_dir):
        os.mkdir(benchmark_dir)

    if 'sleep' in args.experiment:
        sample = ET.parse(os.path.join(CHAPPIE_PATH, 'config', 'SLEEP.xml'))
    elif 'poll' in args.experiment:
        sample = ET.parse(os.path.join(CHAPPIE_PATH, 'config', 'POLL.xml'))
    else:
        sample = ET.parse(os.path.join(CHAPPIE_PATH, 'config', 'SAMPLE.xml'))
    sample_root = sample.getroot()

    import operator

    from functools import reduce

    chappie_params = experiment['chappie']
    chappie_keys = [tuple(chappie_params.keys())] * reduce(operator.mul, map(len, chappie_params.values()))
    chappie_values = product(*chappie_params.values())

    for keys, values in zip(chappie_keys, product(*chappie_params.values())):
        name = ''
        vm_rate, os_rate = values
        tr = np.gcd(vm_rate, os_rate)

        keys = ('timerRate', ) + keys

        values = (tr, vm_rate // tr, os_rate // tr)

        for key, value in zip(keys, values):
            try:
                sample_root.find(key).text = str(value)
                name += '{}{}_'.format(key, value)
            except:
                pass

        name = name[:-1]
        # print(config_dir)
        sample.write(os.path.join(config_dir, name + '.xml'))

    for k, case in enumerate(experiment['cases']):
        benchmark = json.load(open(os.path.join(CHAPPIE_PATH, 'benchmark', '{}.json'.format(case['config'])), 'r'))
        benchmark.update(case)
        json.dump(benchmark, open(os.path.join(benchmark_dir, '{}_{}.json'.format(k + 1, benchmark['benchmark'])), 'w'))

    print(work_dir)

if __name__ == '__main__':
    main()
