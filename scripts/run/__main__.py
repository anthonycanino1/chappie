import argparse
import json
import os
import shutil

from argparse import Namespace
from os.path import dirname

run_libs = dirname(__file__)
chappie_root = dirname(dirname(run_libs))

def set_default(config_path):
    shutil.copyfile(config_path, os.path.join(run_libs, 'default_config.json'))

def load_default(args):
    default = load_config(os.path.join(run_libs, 'default_config.json'))

    if args.work_directory:
        default['work_directory'] = args.work_directory

    if 'properties' in default:
        default['properties'] += args.properties
    else:
        default['properties'] = args.properties

    if 'class_path' in default:
        default['class_path'] += args.class_path
    else:
        default['class_path'] = args.class_path

    if args.main:
        default['main'] = args.main

    if args.args:
        default['args'] = args.args

    return default

def load_config(path):
    return json.load(open(path))

def parse_args():
    parser = argparse.ArgumentParser()

    parser.add_argument('-cfg', '--config')
    parser.add_argument('-dir', '--work-directory')

    # parser.add_argument('--set-default', action = 'store_true')

    args = parser.parse_args()

    if args.work_directory and os.path.exists(os.path.join(args.work_directory, 'config.json')):
        config = json.load(open(os.path.join(args.work_directory, 'config.json')))
    elif args.config:
        config = json.load(open(args.config))
    else:
        config = json.load(open(os.path.join(dirname(__file__), 'default_config.json')))

        jparser = parser.add_subparsers().add_parser(name = "java args")
        jparser.add_argument('-dargs', '--properties', nargs = '+', default = [])
        jparser.add_argument('-cp', '--class-path', default = None)
        jparser.add_argument('main', default = '')
        jparser.add_argument('args', nargs = argparse.REMAINDER, default = [])

        jargs = jparser.parse_args()

    if args.work_directory:
        config['work_directory'] = args.work_directory
    elif 'work_directory' not in config:
        config['work_directory'] = 'chappie-logs'

    # if args.set_default:
    #     set_default(args.config)

    return config

def build_call(config):
    call_args = {}
    call_args['chappie_root'] = chappie_root
    call_args['class_path'] = config['class_path']
    call_args['main'] = config['main']

    if 'properties' in config and config['properties']:
        call_args['properties'] = '-D' + ' -D'.join(config['properties'])
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

    if 'hp' not in config:
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

    # -agentpath:{chappie_root}/build/liblagent.so=interval={hp},logPath={work_directory}/raw/method.csv
    java_call = """
    java -Xbootclasspath/a:{chappie_root}/chappie.jar
        -agentpath:{chappie_root}/build/liblagent.so
        -javaagent:{chappie_root}/chappie.jar
        {properties}
        -cp {chappie_root}/chappie.jar:{class_path}
        {main} {args}
    """.format(**call_args)

    print(java_call)

if __name__ == "__main__":
    build_call(parse_args())
