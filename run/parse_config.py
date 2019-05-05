#!/usr/bin/python3

import argparse
import json
import os

import xml.etree.ElementTree as ET

def parse_args():
    parser = argparse.ArgumentParser()
    parser.add_argument('-config')
    parser.add_argument('-experiment')

    return parser.parse_args()

def parse_config(config, experiment):
    config = json.load(open(args.config, 'r'))

    root = ET.parse(args.experiment).getroot()
    for child in root:
        try:
            config[child.tag] = int(child.text)
        except:
            config[child.tag] = child.text

    return config

if __name__ == '__main__':
    args = parse_args()
    config = parse_config(args.config, args.experiment)

    work_path = config['workPath']

    # setup the paths
    path = None
    for dir in work_path.split(os.sep):
        if path is None:
            path = dir
        else:
            path = os.path.join(path, dir)
        if not os.path.exists(path):
            os.mkdir(path)

    config['config'] = args.experiment
    config['jar'] = config['jar'].format(**config)
    config['class_path'] = '{chappie_path}/chappie.jar:{chappie_path}/vendor/jna-4.5.0.jar:{jar}'.format(**config)
    config['hpRate'] = config['timerRate'] * config['hpFactor']
    config['hp_args'] = config['agent'].format(**config)
    config['args'] = config['args'].format(**config)

    command = '{java_path} -cp {class_path} -agentpath:{hp_args} {dargs} {main} {args}'.format(**config)
    print(command)
