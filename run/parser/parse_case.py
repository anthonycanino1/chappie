#!/usr/bin/python3

import argparse
import json
import os

import xml.etree.ElementTree as ET

def parse_args():
    parser = argparse.ArgumentParser()
    parser.add_argument('-benchmark')
    parser.add_argument('-config')

    return parser.parse_args()

def parse_config(benchmark, config):
    config = json.load(open(benchmark, 'r'))

    root = ET.parse(args.experiment).getroot()
    for child in root:
        try:
            config[child.tag] = int(child.text)
        except:
            config[child.tag] = child.text

    return config

if __name__ == '__main__':
    args = parse_args()
    benchmark = json.load(open(args.benchmark, 'r'))

    root = ET.parse(args.config).getroot()
    benchmark['workPath'] = root.find('workPath').text
    benchmark['mode'] = root.find('mode').text

    # setup the paths
    path = None
    for dir in benchmark['workPath'].split(os.sep):
        if path is None:
            path = dir
        else:
            path = os.path.join(path, dir)
        if not os.path.exists(path):
            os.mkdir(path)

    benchmark['config'] = args.config

    benchmark['jar'] = benchmark['jar'].format(**benchmark)
    benchmark['class_path'] = '{chappie_path}/chappie.jar:{chappie_path}/vendor/jna-4.5.0.jar:{jar}'.format(**benchmark)
    benchmark['dargs'] = benchmark['dargs'].format(**benchmark)
    benchmark['args'] = benchmark['args'].format(**benchmark)

    if benchmark['mode'] == 'NOP':
        command = '{java_path} -cp {class_path} {dargs} {main} {args}'.format(**benchmark)
    else:
        benchmark['hpRate'] = int(root.find('timerRate').text) * int(root.find('hpFactor').text)
        benchmark['hp_args'] = benchmark['agent'].format(**benchmark)
        command = '{java_path} -agentpath:{hp_args} -cp {class_path} {dargs} {main} {args}'.format(**benchmark)

    print(command)
