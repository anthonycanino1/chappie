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
    jiff['jiffies'] = jiff[[col for col in jiff.columns if col not in ('epoch', 'cpu')]].sum(axis = 1)

    jiff.loc[jiff['cpu'] < 20, 'socket'] = 1
    jiff.loc[jiff['cpu'] >= 20, 'socket'] = 2

    jiff['jiffies'] = jiff.groupby('cpu').jiffies.diff().fillna(0).astype(int)

    return jiff.set_index(['epoch', 'socket'])[['jiffies']]

def jiffies_smoothing(df):
    app = sys = 0
    records = []
    for epoch, socket, app_jiff, sys_jiff in df[['epoch', 'socket', 'app', 'sys']].values:
        app += app_jiff
        sys += sys_jiff
        if app_jiff > 0 and app <= sys:
            records.append([epoch, socket, app, sys])
            app = sys = 0

    grp = pd.DataFrame(data = records, columns = ['epoch', 'socket', 'app', 'sys']).set_index(['epoch', 'socket'])
    grp['jiffies'] = grp.app / grp.sys

    df = pd.concat([df.set_index(['epoch', 'socket']), grp], axis = 1).bfill().fillna(0)
    df = df[df.jiffies > 0]

    return df.reset_index()[['epoch', 'jiffies']]

def align(app, sys):
    jiff = pd.concat([
        app.groupby(['epoch', 'socket']).jiffies.sum(),
        sys.groupby(['epoch', 'socket']).jiffies.sum()
    ], axis = 1)
    jiff.columns = ['app', 'sys']
    jiff = jiff.reset_index().groupby('socket').apply(jiffies_smoothing)

    jiff = pd.merge(app.reset_index(), jiff, on = ['epoch', 'socket'], suffixes = ('_', ''))
    jiff.socket = jiff.socket.astype(int)

    return jiff.set_index(['epoch', 'tid', 'os_name'])[['socket', 'jiffies']]

def process(app, name, sys):
    app = process_app(app, name)
    sys = process_sys(sys)
    jiff = align(app, sys)

    return jiff
