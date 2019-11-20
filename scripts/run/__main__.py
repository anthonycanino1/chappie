import argparse
import json
import os
import sys

from argparse import Namespace
from os.path import dirname, realpath

chappie_root = realpath(dirname(dirname(dirname(__file__))))

def parse_args():
    parser = argparse.ArgumentParser()

    # where we place the data; this should be independent of
    # everything else
    parser.add_argument('-d', '--work-directory', default = './chappie-logs')
    parser.add_argument('--samples', default = 1)

    group = parser.add_mutually_exclusive_group()

    group.add_argument('-cp', '--classpath')
    # this is just used for my testing
    group.add_argument('--config')

    args, _ = parser.parse_known_args()
    samples = args.samples

    if args.classpath is None and args.config is None:
        if os.path.exists(os.path.join(args.work_directory, 'config.json')):
            config = json.load(open(os.path.join(args.work_directory, 'config.json')))
            config['work_directory'] = args.work_directory
            config['samples'] = samples

            return config
        else:
            raise ValueError('no arguments or config specified')
    elif not os.path.exists(os.path.dirname(args.work_directory)):
        raise FileNotFoundError('target for work directory ({}) is not available'.format(args.work_directory))
    elif not os.path.exists(args.work_directory):
        os.mkdir(args.work_directory)
    elif not os.path.isdir(args.work_directory):
        raise FileExistsError('target for work directory ({}) is not available'.format(args.work_directory))

    if not os.path.exists(os.path.join(args.work_directory, 'raw')):
        os.mkdir(os.path.join(args.work_directory, 'raw'))

    # grab the config if it's available
    if args.config is not None:
        config = json.load(open(args.config))
        config['work_directory'] = args.work_directory
        config['samples'] = samples

        json.dump(config, open(os.path.join(args.work_directory, 'config.json'), 'w'))

        return config

    # otherwise let's parse out java-like args
    parser = argparse.ArgumentParser()

    parser.add_argument('-d', '--work-directory', default = 'chappie-logs')
    parser.add_argument('--samples', default = 1)

    parser.add_argument('-cp')

    args, java_args = parser.parse_known_args()

    xargs = [arg for arg in java_args if '-X' in arg]
    dargs = [arg for arg in java_args if '-D' in arg]

    app_args = [arg for arg in java_args if arg not in xargs + dargs]

    xargs = [arg.replace('-X', '') for arg in xargs]

    dargs = [arg.replace('-D', '').replace('-Darg=', '').replace(':', '=').split(',') for arg in dargs]
    dargs = [arg for args in dargs for arg in args] + ['chappie.dir={}'.format(args.work_directory)]

    java_main, app_args = app_args[0], app_args[1:]

    config = {
        'xargs': xargs,
        'dargs': dargs,
        'classpath': args.cp,
        'main': java_main,
        'args': app_args,
        'work_directory': args.work_directory,
        'samples': samples
    }

    json.dump(config, open(os.path.join(args.work_directory, 'config.json'), 'w'))

    return config

def build_java_call(config):
    java_args = {'root': chappie_root, 'work_directory': config['work_directory']}

    if 'xargs' in config and len(config['xargs']) > 0:
        xargs = '-X' + ' -X'.join(config['xargs'])
        java_args['xargs'] = xargs
    else:
        java_args['xargs'] = '-Xbootclasspath/a:'

    java_args['xargs'] = java_args['xargs'].replace('-Xbootclasspath/a:', '-Xbootclasspath/a:{}/chappie.jar'.format(chappie_root))

    if 'dargs' in config:
        dargs = '-D' + ' -D'.join(config['dargs'])
        java_args['dargs'] = dargs
    else:
        java_args['dargs'] = ''

    if 'chappie' in config:
        java_args['dargs'] += ' '.join('-Dchappie.{}={}'.format(*i) for i in config['chappie'].items())

    java_args['classpath'] = config['classpath']
    java_args['main'] = config['main']
    java_args['args'] = ' '.join(config['args']).format(**java_args)

    java_args['samples'] = config['samples']

    if 'rate=0' not in java_args['dargs']:
        java_call = """
            java
                {xargs}
                -javaagent:{root}/chappie.jar
                -agentpath:{root}/build/liblagent.so=logPath={work_directory}/raw/method.csv,intervalMin=5,intervalMax=13,samples={samples}
                {dargs}
                -cp {root}/chappie.jar:{classpath}
                {main} {args}
        """.format(**java_args)
    else:
        java_call = """
            java
                {xargs}
                {dargs}
                -cp {root}/chappie.jar:{classpath}
                {main} {args}
        """.format(**java_args)

    return java_call

def main(args):
    call = build_java_call(args)

    print(call)

if __name__ == "__main__":
    main(parse_args())
