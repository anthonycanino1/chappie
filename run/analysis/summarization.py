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
    'VM Thread'
)

jvm_names = (
    'Common-Cleaner',
    'Reference Handl',
    'Signal Dispatch',
    'Finalizer',
    'process reaper'
)

chappie_names = (
    'Chaperone',
    'Honest Profiler'
)

if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('-path')

    args = parser.parse_args()

    # setup the paths
    if not os.path.exists(args.path):
        raise FileNotFoundError('No directory at {}'.format(os.path.abspath(args.path)))

    os.chdir(args.path)

    runtime = pd.read_csv(os.path.join('experiment', 'processed', 'chappie.runtime.csv'))
    runtime['name'] = runtime['name'].map({
        'package1': 'package',
        'package2': 'package',
        'dram1': 'dram',
        'dram2': 'dram',
        'runtime': 'runtime'
    })
    runtime = runtime.groupby('name').agg(['mean', 'std']).T
    runtime['runtime'] /= 1000

    reference = pd.read_csv(os.path.join('reference', 'processed', 'chappie.runtime.csv'))
    reference['name'] = reference['name'].map({
        'package1': 'package',
        'package2': 'package',
        'dram1': 'dram',
        'dram2': 'dram',
        'runtime': 'runtime'
    })
    reference = reference.groupby('name').agg(['mean', 'std']).T
    reference['runtime'] /= 1000

    runtime['package_reference'] = reference['package']
    runtime['package_overhead'] = (runtime['package'] - runtime['package_reference']) / runtime['package_reference']

    runtime['dram_reference'] = reference['dram']
    runtime['dram_overhead'] = (runtime['dram'] - runtime['dram_reference']) / runtime['dram_reference']

    runtime['time_reference'] = reference['runtime']
    runtime['time_overhead'] = (runtime['runtime'] - runtime['time_reference']) / runtime['time_reference']

    runtime = runtime.T.sort_index().T

    # runtime['runtime_error'] = (runtime['runtime'] - runtime['reference']) / runtime['reference']

    # runtime = pd.concat([runtime, reference])

    # try:
    #     iters = range(max([int(re.search(r'\d+', f).group()) for f in os.listdir()]))
    # except:
    #     iters = ['']

    # if args.destination is None:
    #     args.destination = os.path.join(args.path, 'summary')
    #     args.path = os.path.join(args.path, 'processed')
    #
    # if not os.path.exists(args.destination):
    #     os.mkdir(args.destination)
    # args.reference = os.path.join(args.reference, 'processed')

    # start = time()
    # # size = (len(os.listdir(args.path)) - 1) / 2
    #
    # runtime = pd.read_csv(os.path.join('processed', 'chappie.runtime.csv'))
    # runtime = pd.concat([
    #     runtime,
    #     pd.read_csv(os.path.join(args.reference, 'processed', 'chappie.runtime.csv'))
    # ])
    #
    # runtime = pd.read_csv(os.path.join(args.path, 'chappie.runtime.csv'), header = None)
    # runtime['experiment'] = 'experiment'
    #
    # ref = pd.read_csv(os.path.join(args.reference, 'chappie.runtime.csv'), header = None)
    # ref['experiment'] = 'reference'
    #
    # runtime = pd.concat([runtime, ref])
    # runtime.columns = ['value', 'experiment']
    #
    # runtime = runtime.groupby('experiment').agg(['mean', 'std'])
    # runtime.columns = runtime.columns.droplevel()
    #
    # ref = runtime['mean'][runtime.index == 'reference'].max()
    # runtime['overhead'] = (runtime['mean'] - ref) / ref
    # runtime['error'] = 1 -(runtime['mean'] - runtime['std']) / runtime['mean']
    #
    # runtime.to_csv(os.path.join(args.destination, 'chappie.runtime.csv'))
    #
    # trace = pd.concat([pd.read_csv(os.path.join(args.path, f)) for f in os.listdir(args.path) if 'energy' in f])
    # trace['package'] = trace['package'].replace(np.inf, 0).replace(-np.inf, 0)
    # trace['dram'] = trace['dram'].replace(np.inf, 0).replace(-np.inf, 0)
    # trace = trace.groupby(['socket'])[['package', 'dram']].sum() / size
    # trace.columns = ['total ' + col for col in trace.columns]
    #
    # thread = pd.concat([pd.read_csv(os.path.join(args.path, f)) for f in os.listdir(args.path) if 'thread' in f])
    # method = thread.copy(deep = True)
    #
    # socket = thread.groupby('socket')[['package', 'dram']].sum() / size
    # socket.columns = ['application ' + col for col in socket.columns]
    # trace = pd.concat([trace, socket], axis = 1)
    # trace['other application package'] = trace['total package'] - trace['application package']
    # trace['other application dram'] = trace['total dram'] - trace['application dram']
    #
    # trace['other application package'] = trace['total package'] - trace['application package']
    # trace['other application dram'] = trace['total dram'] - trace['application dram']
    #
    # thread = thread.groupby(['socket', 'thread'])[['package', 'dram']].sum() / size
    # thread = thread.reset_index()
    #
    # system_threads = thread[thread['thread'].str.contains('|'.join(jit_names + gc_names)) | (thread['thread'].isin(other_names))]
    # jvm_threads = thread[thread['thread'].isin(jvm_names)]
    # chappie_threads = thread[thread['thread'].isin(chappie_names)]
    #
    # non_application_names = np.concatenate([names['thread'].unique() for names in (system_threads, jvm_threads, chappie_threads)])
    # application_threads = thread[~thread['thread'].isin(non_application_names)]
    #
    # system_stats = system_threads.groupby('socket').sum()[['package', 'dram']]
    # system_stats.columns = ['system ' + col for col in system_stats.columns]
    #
    # jvm_stats = jvm_threads.groupby('socket').sum()[['package', 'dram']]
    # jvm_stats.columns = ['jvm ' + col for col in jvm_stats.columns]
    #
    # chappie_stats = chappie_threads.groupby('socket').sum()[['package', 'dram']]
    # chappie_stats.columns = ['chappie ' + col for col in chappie_stats.columns]
    #
    # application_stats = application_threads.groupby('socket').sum()[['package', 'dram']]
    # application_stats.columns = ['application ' + col for col in application_stats.columns]
    #
    # trace = trace[['total package', 'total dram', 'other application package', 'other application dram']]
    # summary = pd.concat([trace, system_stats, jvm_stats, chappie_stats, application_stats], axis = 1)
    # for col in summary.columns:
    #     summary[col] = summary[col].replace(np.inf, 0).replace(-np.inf, 0)
    # summary.to_csv(os.path.join(args.destination, 'chappie.component.csv'))
    #
    # method['energy'] = method['package'] + method['dram']
    #
    # dfs = []
    # for col in ('all', 'unfiltered', 'deep'):
    #     df = pd.DataFrame()
    #     df = method[(method[col] != 'end') & (method['thread'] != 'Chaperone')].copy(deep = True)
    #     df['top'] = df[col].str.split(';').map(lambda x: x[0])
    #
    #     df['method'] = df['top'].str.split('.').map(lambda x: '.'.join(x[-2:]))
    #     df['class'] = df['top'].str.split('.').map(lambda x: x[-2])
    #     df['package'] = df['top'].str.split('.').map(lambda x: '.'.join(x[:-2]))
    #     df['type'] = col
    #
    #     tdfs = []
    #
    #     df['method'] = df[col].str.split(';').map(lambda x: x[0])
    #     df['context'] = df[col].str.split(';').map(lambda x: x[:2])
    #     df['context'] = df['context'].map(lambda x: x[1] if isinstance(x, list) and len(x) > 1 else 'end')
    #
    #     tdf = df.groupby(['method', 'context'])['energy'].agg(('sum', 'count')).reset_index()
    #     tdf['energy'] = tdf['sum']
    #
    #     tdf['energy'] /= tdf['energy'].sum()
    #     tdf['energy'] *= 100
    #     tdf['Energy'] = tdf['energy']
    #
    #     tdf['count'] /= tdf['count'].sum()
    #     tdf['count'] *= 100
    #     tdf['Time'] = tdf['count']
    #
    #     tdf['level'] = 'context'
    #     tdf['type'] = col
    #     tdf['name'] = tdf['method'] # .str.cat(tdf['context'])
    #
    #     tdfs.append(tdf)
    #
    #     for type in ('method', 'class', 'package'):
    #         tdf = df.groupby(type)['energy'].agg(('sum', 'count')).reset_index()
    #
    #         tdf['energy'] = tdf['sum']
    #         tdf['energy'] /= tdf['energy'].sum()
    #         tdf['energy'] *= 100
    #         tdf['Energy'] = tdf['energy']
    #
    #         tdf['count'] /= tdf['count'].sum()
    #         tdf['count'] *= 100
    #         tdf['Time'] = tdf['count']
    #
    #         tdf['level'] = type
    #         tdf['type'] = col
    #         tdf['name'] = tdf[type]
    #
    #         tdfs.append(tdf)
    #
    #     df = pd.concat(tdfs, sort = True)
    #
    #     df = df[['level', 'type', 'name', 'context', 'Energy', 'Time']]
    #     dfs.append(df)
    #
    # method = pd.concat(dfs)
    # method['Energy'] = method['Energy'].fillna(0)
    # method['context'] = method['context'].fillna('end')
    #
    # method.to_csv(os.path.join(args.destination, 'chappie.method.csv'), index = False)
    #
    # print('{:.2f} seconds for summary'.format(time() - start))
