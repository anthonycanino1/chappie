import numpy as np
import pandas as pd

def process_app(jiff, name):
    name = {int(k): v for k, v in name.items()}

    jiff['os_name'] = jiff.tid.map(name).fillna("").astype(str)
    mask = jiff.tid == jiff[jiff.os_name == 'java'].tid.max()
    jiff.loc[mask, 'os_name'] = 'main'

    jiff.loc[jiff['cpu'] < 20, 'socket'] = 1
    jiff.loc[jiff['cpu'] >= 20, 'socket'] = 2

    jiff = jiff.sort_values(['epoch', 'tid'])

    jiff['jiffies'] = jiff.user + jiff.sys
    jiff['jiffies'] = jiff.groupby('tid').jiffies.diff().fillna(0).astype(int)

    return jiff.set_index(['epoch', 'tid', 'os_name'])[['socket', 'jiffies']]

def process_sys(jiff):
    jiff = jiff.drop(columns = 'idle')
    jiff['jiffies'] = jiff[[col for col in jiff.columns if col not in ('epoch', 'cpu')]].sum(axis = 1)

    jiff.loc[jiff['cpu'] < 20, 'socket'] = 1
    jiff.loc[jiff['cpu'] >= 20, 'socket'] = 2

    jiff['jiffies'] = jiff.groupby('cpu').jiffies.diff().fillna(0).astype(int)

    return jiff.set_index(['epoch', 'socket'])[['jiffies']]

def jiffies_smoothing(df):
    app = sys = 0
    records = []
    # print(df.set_index(['epoch', 'socket']))
    # print(df.sum())
    i = 0
    for epoch, socket, app_jiff, sys_jiff in df[['epoch', 'socket', 'app', 'sys']].values:
        app += app_jiff
        sys += sys_jiff
        i += 1
        if (app > 0 and app <= sys) or i % 10:
            records.append([epoch, socket, min(int(app), int(sys)), sys])
            app = sys = 0
            i = 0

    # if app > 0 and sys > 0:
    #     records.append([epoch, socket, app, sys])
    grp = pd.DataFrame(data = records, columns = ['epoch', 'socket', 'app', 'sys']).set_index(['epoch', 'socket'])
    grp['jiffies'] = (grp.app / grp.sys).fillna(1).replace(np.inf, 1).clip(0, 1)
    print(grp.groupby(['epoch', 'socket']).jiffies.sum().mean())
    print(grp.sum())

    df = pd.concat([df.set_index(['epoch', 'socket']), grp], axis = 1).bfill().fillna(0)
    df = df[df.jiffies > 0]

    return df.reset_index()[['epoch', 'jiffies']]

def align(app, sys):
    jiff = pd.concat([
        app.groupby(['epoch', 'socket']).jiffies.sum(),
        sys.groupby(['epoch', 'socket']).jiffies.sum()
    ], axis = 1)
    jiff.columns = ['app', 'sys']
    jiff.to_csv('pre-align-jiffies.csv')
    jiff = jiff.reset_index().groupby('socket').apply(jiffies_smoothing)
    jiff.to_csv('post-align-jiffies.csv')

    jiff = pd.merge(app.reset_index(), jiff, on = ['epoch', 'socket'], suffixes = ('_', ''))
    jiff.socket = jiff.socket.astype(int)

    return jiff.set_index(['epoch', 'tid', 'os_name'])[['socket', 'jiffies']]

def process(app, name, sys):
    app = process_app(app, name)
    sys = process_sys(sys)
    jiff = align(app, sys)

    return jiff
