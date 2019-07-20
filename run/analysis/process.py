#!/usr/bin/python3

import argparse
import json
import os

import xml.etree.ElementTree as ET

from attribution import *
from runtime import *

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

def main():
    args = parse_args()
    case = parse_config(args.benchmark, args.config)

    work_dir = os.path.join(
        os.path.dirname(os.path.dirname(args.benchmark)),
        os.path.splitext(os.path.basename(args.benchmark))[0],
        os.path.splitext(os.path.basename(args.config))[0]
    )
    if not os.path.exists(work_dir):
        os.makedirs(work_dir)

    # setup the directories
    path = work_dir
    if not os.path.exists(path):
        raise FileNotFoundError('No directory at {}'.format(os.path.abspath(path)))
    if not os.path.exists(os.path.join(path, 'processed')):
        os.mkdir(os.path.join(path, 'processed'))

    iters = tuple(range(case['iters'] // case['warm_up_fraction'], case['iters']))
    main_id = process_runtime(path, iters)
    if case['mode'] != 'NOP':
        process_rates(path, iters)
        if case['mode'] in 'SAMPLE':
            attribute(path, iters, main_id)
            pass
        print('processed!')
    else:
        print('processed!')

if __name__ == '__main__':
    main()
