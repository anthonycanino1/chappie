#!/usr/bin/python3

import argparse
import os

from summarize import *

def parse_args():
    parser = argparse.ArgumentParser()
    parser.add_argument('-benchmark')

    return parser.parse_args()

def main():
    args = parse_args()

    path = args.benchmark

    target = os.path.join(args.benchmark, 'summary')
    if not os.path.exists(target):
        os.mkdir(target)

    print(path)
    runtime = runtime_summary(path)
    try:
        runtime['attributed'] = attribution_summary(path)
        print(runtime.round(2).fillna('-'))
        runtime.to_csv(os.path.join(target, 'chappie.runtime.csv'))

        component = component_summary(path)
        component.to_csv(os.path.join(target, 'chappie.component.csv'))

        # print('?')
        method = method_summary(path)
        print(method.head(10))
        method.to_csv(os.path.join(target, 'chappie.method.csv'), index = False)
    except Exception as e:
        print(e)
        print(runtime[['rate', 'rate_std']].round(2).fillna('-'))
        runtime.to_csv(os.path.join(target, 'chappie.runtime.csv'))

if __name__ == '__main__':
    main()
