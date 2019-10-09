import argparse
import json
import os

from argparse import Namespace
from os.path import dirname

run_libs = dirname(__file__)
chappie_root = dirname(dirname(run_libs))

def parse_args():
    parser = argparse.ArgumentParser()

    parser.add_argument('-cfg', '--config')
    parser.add_argument('-dir', '--work-directory')

    args = parser.parse_args()

    if args.config:
        config = json.load(open(args.config))
    elif args.work_directory and os.path.exists(os.path.join(args.work_directory, 'config.json')):
        config = json.load(open(os.path.join(args.work_directory, 'config.json')))
    else:
        raise ArgumentError('no config!')

    if args.work_directory:
        config['work_directory'] = args.work_directory
    elif 'work_directory' not in config:
        config['work_directory'] = 'chappie-logs'

    return config

def build_java_call(config):
    call_args = {}
    call_args['chappie_root'] = chappie_root
    call_args['class_path'] = config['class_path']
    call_args['main'] = config['main']

    if 'properties' in config and config['properties']:
        call_args['properties'] = '-D' + ' -D'.join(config['properties']) + ' '
    else:
        call_args['properties'] = ''

    if 'chappie' in config:
        chappie_args = config['chappie']
        if isinstance(config['chappie'], dict):
            if 'hp' in config['chappie']:
                call_args['hp'] = config['chappie']['hp']
            chappie_args = ['chappie.{}={}'.format(k, v) for k, v in config['chappie'].items()]
        else:
            call_args['hp'] = [darg.split('=')[1] for darg in config['properties'] if 'chappie.hp' in darg]
            chappie_args = config['chappie']

        call_args['properties'] += '-D' + ' -D'.join(chappie_args)

    if 'hp' not in config['chappie']:
        call_args['hp'] = 4

    if isinstance(config['args'], list):
        call_args['args'] = ' '.join(config['args'])

    if not os.path.exists(config['work_directory']):
        os.mkdir(config['work_directory'])
    if not os.path.exists(os.path.join(config['work_directory'], 'raw')):
        os.mkdir(os.path.join(config['work_directory'], 'raw'))
    json.dump(config, open(os.path.join(config['work_directory'], 'config.json'), 'w'), indent = 2)

    call_args['work_directory'] = config['work_directory']

    call_args['properties'] += ' -Dchappie.dir={}'.format(call_args['work_directory'])
    call_args['args'] = config['args'].format(work_directory = call_args['work_directory'])

    java_call = """
        java
            -Xbootclasspath/a:{chappie_root}/chappie.jar
            -Xmx16g
            -javaagent:{chappie_root}/chappie.jar
            -agentpath:{chappie_root}/build/liblagent.so=logPath={work_directory}/raw/method.csv,interval={hp}
            {properties}
            -cp {chappie_root}/chappie.jar:{class_path}
            {main} {args}
    """.format(**call_args)

    # java_call = """java -cp {class_path} {main} {args}""".format(**call_args)

    return java_call

def main(args):
    call = build_java_call(args)

    print(call)

if __name__ == "__main__":
    main(parse_args())
