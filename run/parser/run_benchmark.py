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

def main():
    args = parse_args()
    benchmark = json.load(open(args.benchmark, 'r'))

    root = ET.parse(args.config).getroot()
    benchmark['mode'] = root.find('mode').text

    work_dir = os.path.join(
        os.path.dirname(os.path.dirname(args.benchmark)),
        os.path.splitext(os.path.basename(args.benchmark))[0],
        os.path.splitext(os.path.basename(args.config))[0]
    )
    if not os.path.exists(work_dir):
        os.makedirs(work_dir)

    benchmark['work_dir'] = work_dir
    benchmark['config'] = args.config
    benchmark['jar'] = benchmark['jar'].format(**benchmark)
    benchmark['class_path'] = '{chappie_path}/chappie.jar:{chappie_path}/vendor/jna-4.5.0.jar:{chappie_path}/vendor/javassist.jar:{jar}'.format(**benchmark)

    benchmark['dargs'] = ['-D{}'.format(darg.format(**benchmark)) for darg in benchmark['dargs']]
    # print(benchmark)
    benchmark['dargs'] = '-Dchappie.config={config} -Dchappie.workDir={work_dir}'.format(**benchmark) + ' ' + ' '.join(benchmark['dargs'])

    benchmark['args'] = benchmark['args'].format(**benchmark)

    if benchmark['mode'] != 'SAMPLE':
        command = '{java_path} -cp {class_path} {vm_options} {dargs} {main} {args}'.format(**benchmark)
    else:
        benchmark['hpRate'] = int(root.find('timerRate').text) * int(root.find('hpFactor').text)
        benchmark['hp_args'] = benchmark['agent'].format(**benchmark)
        # command = '{java_path} {vm_options} -cp {class_path} -agentpath:{hp_args} {dargs} {main} {args}'.format(**benchmark)
        command = '{java_path} {vm_options} -Xbootclasspath/a:{chappie_path}/chappie.jar:{chappie_path}/vendor/javassist.jar -cp {class_path} -javaagent:{chappie_path}/chappie.jar -agentpath:{hp_args} {dargs} {main} {args}'.format(**benchmark)

    print(command)

if __name__ == '__main__':
    main()
