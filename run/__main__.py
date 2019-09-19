import argparse
import os

from os.path import dirname

chappie_root = dirname(dirname(__file__))

def parse_args():
    parser = argparse.ArgumentParser()

    parser.add_argument('-dir', '--work-directory', default = 'data')

    parser.add_argument('-dargs', '--properties', nargs = '+')
    parser.add_argument('-cp', '--class-path', required = True)
    parser.add_argument('main')
    parser.add_argument('args', nargs = argparse.REMAINDER)

    args = parser.parse_args()

    return args

def build_call(args):
    args.chappie_root = chappie_root

    if args.properties:
        args.hp = [darg.split('=')[1] for darg in args.properties if 'chappie.hp' in darg]
        args.properties = '-D' + ' -D'.join(args.properties)
    else:
        args.hp = 4
        args.properties = ""

    if not args.args:
        args.args = ""

    if not os.path.exists(args.work_directory):
        os.mkdir(args.work_directory)

    java_call = """
    java -Xbootclasspath/a:{chappie_root}/chappie.jar
    -agentpath:{chappie_root}/build/liblagent.so=interval={hp},logPath={work_directory}/method.csv
    -javaagent:{chappie_root}/chappie.jar
    {properties}
    -cp {chappie_root}/chappie.jar:{class_path}
    {main} {args}
    """.format(
        **vars(args)
    )

    print(java_call)

if __name__ == "__main__":
    build_call(parse_args())
