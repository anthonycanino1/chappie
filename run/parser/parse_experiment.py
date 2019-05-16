#!/usr/bin/python3

import argparse
import json
import os
import xml.etree.ElementTree as ET

from collections import OrderedDict
from itertools import product

import dicttoxml

chappie_run_path = os.path.dirname(os.path.dirname(__file__))

def parse_args():
    parser = argparse.ArgumentParser()
    parser.add_argument('-experiment')

    return parser.parse_args()

if __name__ == '__main__':
    args = parse_args()

    experiment = json.load(open(args.experiment, 'r'), object_pairs_hook = OrderedDict)
    path = experiment['workDir']

    if not os.path.exists(path):
        os.mkdir(path)
    if not os.path.exists(os.path.join(path, 'config')):
        os.mkdir(os.path.join(path, 'config'))
    if not os.path.exists(os.path.join(path, 'benchmark')):
        os.mkdir(os.path.join(path, 'benchmark'))

    benchmark_path = experiment['benchmark']['config_path'].format(root = chappie_run_path)
    benchmark = json.load(open(benchmark_path, 'r'))
    benchmark.update(experiment['benchmark'])
    json.dump(benchmark, open(os.path.join(path, 'benchmark', 'benchmark.json'), 'w'))

    nop = ET.parse(os.path.join(chappie_run_path, 'config', 'NOP.xml'))
    nop_root = nop.getroot()
    nop_root.find('workPath').text = os.path.join(path, 'reference')
    nop.write(os.path.join(path, 'config', 'NOP.xml'))

    chappie_config = experiment['chappie']
    sample = ET.parse(os.path.join(chappie_run_path, 'config', 'SAMPLE.xml'))
    sample_root = sample.getroot()

    for parms in product(*chappie_config.values()):
        name = '_'.join(str(p) for p in parms)
        sample_root.find('workPath').text = os.path.join(path, name)

        for key, value in zip(chappie_config, parms):
            sample_root.find(key).text = str(value)

        sample.write(os.path.join(path, 'config', name + '.xml'))

    print(path)
