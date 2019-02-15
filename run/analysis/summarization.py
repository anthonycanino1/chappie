#!/usr/bin/python3

import argparse
import os

from time import time

import numpy as np
import pandas as pd

jit_names = ('CompilerThre', )
gc_names = ('Main Marker', 'Marker#', 'Refine#', 'Young RemSet', 'GC Thread#')

other_names = (
    'Service Thread',
    'Sweeper thread',
    'VM Periodic Tas',
    'VM Thread',
    'process reaper'
)

jvm_names = (
    'Common-Cleaner',
    'Reference Handl',
    'Signal Dispatch',
    'Finalizer',
)

chappie_names = (
    'Chaperone',
    'Honest Profiler'
)

if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('-path', default = "chappie_test")
    parser.add_argument('-destination', default = None)
    args = parser.parse_args()

    # setup the paths
    if not os.path.exists(args.path):
        raise FileNotFoundError('No directory at {}'.format(os.path.abspath(args.path)))

    if args.destination is None:
        args.destination = os.path.join(args.path, 'summary')
        args.path = os.path.join(args.path, 'processed')

    if not os.path.exists(args.destination):
        os.mkdir(args.destination)

    start = time()
    size = len(os.listdir(args.path)) / 2

    trace = pd.concat([pd.read_csv(os.path.join(args.path, f)) for f in os.listdir(args.path) if 'energy' in f])
    trace = trace.groupby(['socket'])[['package', 'dram']].sum() / size
    trace.columns = ['total ' + col for col in trace.columns]

    thread = pd.concat([pd.read_csv(os.path.join(args.path, f)) for f in os.listdir(args.path) if 'thread' in f])
    method = thread.copy(deep = True)

    socket = thread.groupby('socket')[['package', 'dram']].sum() / size
    socket.columns = ['application ' + col for col in socket.columns]
    trace = pd.concat([trace, socket], axis = 1)
    trace['other application package'] = trace['total package'] - trace['application package']
    trace['other application dram'] = trace['total dram'] - trace['application dram']

    trace['other application package'] = trace['total package'] - trace['application package']
    trace['other application dram'] = trace['total dram'] - trace['application dram']

    thread = thread.groupby(['socket', 'thread'])[['package', 'dram']].sum() / size
    thread = thread.reset_index()

    system_threads = thread[thread['thread'].str.contains('|'.join(jit_names + gc_names)) | (thread['thread'].isin(other_names))]
    jvm_threads = thread[thread['thread'].isin(jvm_names)]
    chappie_threads = thread[thread['thread'].isin(chappie_names)]

    non_application_names = np.concatenate([names['thread'].unique() for names in (system_threads, jvm_threads, chappie_threads)])
    application_threads = thread[~thread['thread'].isin(non_application_names)]

    system_stats = system_threads.groupby('socket').sum()[['package', 'dram']]
    system_stats.columns = ['system ' + col for col in system_stats.columns]

    jvm_stats = jvm_threads.groupby('socket').sum()[['package', 'dram']]
    jvm_stats.columns = ['jvm ' + col for col in jvm_stats.columns]

    chappie_stats = chappie_threads.groupby('socket').sum()[['package', 'dram']]
    chappie_stats.columns = ['chappie ' + col for col in chappie_stats.columns]

    application_stats = application_threads.groupby('socket').sum()[['package', 'dram']]
    application_stats.columns = ['application ' + col for col in application_stats.columns]

    trace = trace[['total package', 'total dram', 'other application package', 'other application dram']]
    summary = pd.concat([trace, system_stats, jvm_stats, chappie_stats, application_stats], axis = 1)
    summary.to_csv(os.path.join(args.destination, 'chappie.component.csv'))

    method['energy'] = method['package'] + method['dram']

    dfs = []
    for col in ('all', 'unfiltered', 'deep'):
        df = pd.DataFrame()
        df = method[(method[col] != 'end') & (method['thread'] != 'Chaperone')].copy(deep = True)
        df['top'] = df[col].str.split(';').map(lambda x: x[0])

        df['method'] = df['top'].str.split('.').map(lambda x: '.'.join(x[-2:]))
        df['class'] = df['top'].str.split('.').map(lambda x: x[-2])
        df['package'] = df['top'].str.split('.').map(lambda x: '.'.join(x[:-2]))
        df['type'] = col

        tdfs = []

        df['context'] = df[col].str.split(';').map(lambda x: ';'.join(x[:2]))
        tdf = df.groupby('context')['energy'].agg(('sum', 'count')).reset_index()
        tdf['sum'] /= tdf['sum'].sum()
        tdf['sum'] *= 100
        tdf['count'] /= tdf['count'].sum()
        tdf['count'] *= 100
        tdf['Energy'] = tdf['sum']
        tdf['Time'] = tdf['count']

        tdf['level'] = 'context'
        tdf['type'] = col
        tdf['name'] = tdf['context']

        tdfs.append(tdf)

        for type in ('method', 'class', 'package'):
            tdf = df.groupby(type)['energy'].agg(('sum', 'count')).reset_index()
            tdf['sum'] /= tdf['sum'].sum()
            tdf['sum'] *= 100
            tdf['count'] /= tdf['count'].sum()
            tdf['count'] *= 100
            tdf['Energy'] = tdf['sum']
            tdf['Time'] = tdf['count']

            tdf['level'] = type
            tdf['type'] = col
            tdf['name'] = tdf[type]

            tdfs.append(tdf)

        df = pd.concat(tdfs)

        df = df[['level', 'name', 'type', 'Energy', 'Time']]
        dfs.append(df)

    method = pd.concat(dfs)
    print(method)
    method.to_csv(os.path.join(args.destination, 'chappie.method.csv'))

    print('{:.2f} seconds for summary'.format(time() - start))
