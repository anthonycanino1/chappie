import os

from logging import getLogger

EXPERIMENT_BASE = {
    "work_dir": "data",
    "experiment": [{
        "type": None,
        "java": {},
        "case": {},
        "chappie": {},
        "warm_up_fraction": 2
    }]
}

JAVA_PARAMS = {
    "java_path": "java",
    "chappie_path": "sunflow",
    "size": "large",
    "iters": 50,
}

CHAPPIE_PARAMS = {
  "java_path": "{java_path}",
  "class_path": "{chappie_path}/chappie.jar:{chappie_path}/vendor/jna-4.5.0.jar:{chappie_path}/vendor/javassist.jar",
  "javaagent": "{chappie_path}/chappie.jar",
  "agentpath": "{chappie_path}/src/async/build/liblagent.so=interval={hpRate},logPath={work_dir}/chappie.stack.csv",
  "dargs": ["chappie.workDir={raw_dir}"]
}

DACAPO_CASE = {
    "config": "dacapo",
    "benchmark": "sunflow",
    "size": "large",
    "iters": 50,
}

RATE_EXPERIMENT = {
    "type": "rate",
    "chappie": {
        "timerRate": [],
        "osFactor": []
    }
}

def validate_config(config):
    logger = getLogger()
    if 'work_dir' not in config or config['work_dir'] == '':
        config['work_dir'] = EXPERIMENT_BASE['work_dir']
        logger.warning("no work directory found; defaulting to '{}'".format(EXPERIMENT_BASE['work_dir']))

    if 'experiment' not in config or 'type' not in config['experiment'] or config['experiment']['type'] == '':
        logger.exception("expected experiment type; found None")
        raise TypeError()

def setup(config):
    logger = getLogger()

    EXPERIMENT_BASE = {
        "work_dir": "data",
        "experiment": [{
            "type": None,
            "java": {},
            "case": {},
            "chappie": {},
            "warm_up_fraction": 2
        }]
    }

    # setup the work directory
    path = config['work_dir']
    if not os.path.exists(path):
        logger = getLogger()
        os.makedirs(path)

    for dir in ('raw', 'processed', 'results', 'config'):
        dir_path = config['{}_dir'.format(dir)] = os.path.join(path, dir)
        if not os.path.exists(dir_path)
            os.makedir(dir_path)

    # setup java call
    java_params = {}
    java_params['raw_dir'] = config['raw_dir']

    # do this for now as a temp hack
    java_params['java_path'] = os.environ['JAVA_HOME']
    java_params['chappie_path'] = os.environ['CHAPPIE_PATH']

    # java_params['java_path'] = config['experiment']['java']['java_path']
    # java_params['chappie_path'] = config['experiment']['java']['chappie_path']

    java_params['vm_options'] = config['experiment']['case']['vm_options'].format(**config['experiment']['case'])

    java_params['jar'] = config['experiment']['case']['jar'].format(**config['experiment']['java'])
    java_params['class_path'] = '{class_path}:{jar}'.format(**config['experiment']['java'])

    java_params['dargs'] = ' '.join('-D{}'.format(darg.format(**java_params)) for darg in config['experiment']['case']['dargs'])

    java_params['main'] = config['experiment']['case']['main']
    java_params['args'] = config['experiment']['case']['args'].format(**config['experiment']['case'])

    if benchmark['mode'] != 'SAMPLE':
        command = '{java_path} {vm_options} -cp {class_path} {dargs} {main} {args}'.format(**java_params)
    else:
        command = '{java_path} {vm_options} -Xbootclasspath/a:{class_path} -javagent:{chappie_path}/chappie.jar -agentpath:{hp_args} -cp {class_path} {dargs} {main} {args}'.format(**java_params)

    print(command)

if __name__ == '__main__':
    main()
