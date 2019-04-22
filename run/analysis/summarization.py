#!/usr/bin/python3

import argparse
import os
import sys

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
    parser.add_argument('-reference', default = "chappie_test")
    parser.add_argument('-destination', default = None)
    args = parser.parse_args()

    if 'plot' in args.path:
        sys.exit(0)

    # setup the paths
    if not os.path.exists(args.path):
        raise FileNotFoundError('No directory at {}'.format(os.path.abspath(args.path)))

    if args.destination is None:
        args.destination = os.path.join(args.path, 'summary')
        args.path = os.path.join(args.path, 'processed')

    if not os.path.exists(args.destination):
        os.mkdir(args.destination)
    args.reference = os.path.join(args.reference, 'processed')

    start = time()
    size = (len(os.listdir(args.path)) - 1) / 2

    runtime = pd.read_csv(os.path.join(args.path, 'chappie.runtime.csv'), header = None)
    runtime['experiment'] = 'experiment'

    ref = pd.read_csv(os.path.join(args.reference, 'chappie.runtime.csv'), header = None)
    ref['experiment'] = 'reference'

    runtime = pd.concat([runtime, ref])
    runtime.columns = ['value', 'experiment']

    runtime = runtime.groupby('experiment').agg(['mean', 'std'])
    runtime.columns = runtime.columns.droplevel()

    ref = runtime['mean'][runtime.index == 'reference'].max()
    runtime['overhead'] = np.abs(runtime['mean'] - ref) / ref
    runtime['error'] = 1 - np.abs(runtime['mean'] - runtime['std']) / runtime['mean']

    # runtime.loc['experiment', 'mean'] = runtime.loc['reference', 'mean']
    # runtime.loc['experiment', 'std'] = runtime.loc['reference', 'std']

    runtime.to_csv(os.path.join(args.destination, 'chappie.runtime.csv'))

    trace = pd.concat([pd.read_csv(os.path.join(args.path, f)) for f in os.listdir(args.path) if 'energy' in f])
    trace['package'] = trace['package'].replace(np.inf, 0).replace(-np.inf, 0)
    trace['dram'] = trace['dram'].replace(np.inf, 0).replace(-np.inf, 0)
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

    # runtime['unique threads'] = application['thread'].unique()
    # runtime['live threads'] = application.groupby('epoch')['thread'].unique().max() / 10

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
    for col in summary.columns:
        summary[col] = summary[col].replace(np.inf, 0).replace(-np.inf, 0)
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

        df['method'] = df[col].str.split(';').map(lambda x: x[0])
        df['context'] = df[col].str.split(';').map(lambda x: x[:2])
        df['context'] = df['context'].map(lambda x: x[1] if isinstance(x, list) and len(x) > 1 else 'end')

        tdf = df.groupby(['method', 'context'])['energy'].agg(('sum', 'count')).reset_index()
        tdf['energy'] = tdf['sum']
        # tdf = df.groupby(['method', 'context'])['energy'].agg('sum').reset_index()

        # print(tdf)
        tdf['energy'] /= tdf['energy'].sum()
        tdf['energy'] *= 100
        tdf['Energy'] = tdf['energy']

        # print(tdf)

        tdf['count'] /= tdf['count'].sum()
        tdf['count'] *= 100
        tdf['Time'] = tdf['count']

        # print(tdf.describe())

        tdf['level'] = 'context'
        tdf['type'] = col
        tdf['name'] = tdf['context']

        print(tdf)

        tdfs.append(tdf)

        for type in ('method', 'class', 'package'):
            tdf = df.groupby(type)['energy'].agg(('sum', 'count')).reset_index()
            # tdf = df.groupby(type)['energy'].agg('sum').reset_index()
            # print(tdf)
            tdf['energy'] = tdf['sum']
            tdf['energy'] /= tdf['energy'].sum()
            tdf['energy'] *= 100
            tdf['Energy'] = tdf['energy']
            tdf['count'] /= tdf['count'].sum()
            tdf['count'] *= 100
            tdf['Time'] = tdf['count']

            tdf['level'] = type
            tdf['type'] = col
            tdf['name'] = tdf[type]

            tdfs.append(tdf)

        df = pd.concat(tdfs, sort = True)

        df = df[['level', 'name', 'type', 'Energy', 'Time']]
        dfs.append(df)

    method = pd.concat(dfs).fillna(0)
    # print(method)

    # filter away context pairs?
    # runtime['methods'] = method['name'].unique()
    # runtime.to_csv(os.path.join(args.destination, 'chappie.runtime.csv'))

    method.to_csv(os.path.join(args.destination, 'chappie.method.csv'), index = False)

    print('{:.2f} seconds for summary'.format(time() - start))
