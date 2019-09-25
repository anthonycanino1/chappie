import pandas as pd

def align_by_tid(vm, jiff):
    vm = vm.reset_index().set_index(['epoch', 'tid'])
    jiff = jiff.reset_index().set_index(['epoch', 'tid'])

    df = pd.concat([vm, jiff], axis = 1).sort_index().reset_index()
    df.jiffies = df.groupby('tid').jiffies.bfill()
    df.socket = df.groupby('tid').socket.bfill()
    df.id = df.groupby('tid').id.bfill().fillna(-1)
    df.tid = df.groupby('tid').tid.bfill()
    df.activeness = df.groupby('tid').activeness.bfill().fillna(1)

    df = df[df.jiffies == df.jiffies]

    df.socket = df.socket.astype(int)
    df.id = df.id.astype(int)
    df.tid = df.tid.astype(int)

    return df.set_index(['epoch', 'id', 'tid'])[['socket', 'activeness', 'jiffies']]

def align_by_name(vm, jiff):
    vm = vm.reset_index()
    jiff = jiff.reset_index()

    df = pd.merge(vm, jiff, on = ('epoch', 'os_name'), how = 'outer', suffixes = ('_', ''))
    df.jiffies = df.groupby('tid').jiffies.bfill()
    df.socket = df.groupby('tid').socket.bfill()
    df.id = df.groupby('tid').id.bfill().fillna(-1)
    df.tid = df.groupby('tid').tid.bfill()
    df.activeness = df.groupby('tid').activeness.bfill().fillna(1)

    df = df[df.jiffies == df.jiffies]

    df.socket = df.socket.astype(int)
    df.id = df.id.astype(int)
    df.tid = df.tid.astype(int)

    return df.set_index(['epoch', 'id', 'tid'])[['socket', 'activeness', 'jiffies']]

def align_state(vm, jiff):
    vm = vm
    jiff = jiff

    mask = vm.index.get_level_values('tid') > -1
    mapped = vm[mask]

    if not mapped.empty:
        tid = align_by_tid(mapped, jiff)

    mask = vm.index.get_level_values('tid') == -1
    unmapped = vm[mask]
    name = align_by_name(unmapped, jiff)

    if not mapped.empty:
        df = pd.concat([tid, name]).sort_index()
    else:
        df = name

    df.activeness = df.activeness.fillna(1)
    df['state'] = df.activeness * df.jiffies
    df['state'] = (df.state / df.groupby('epoch').state.sum()).fillna(0)

    return df.reset_index().set_index(['epoch', 'id', 'tid'])[['socket', 'state']]

def align_energy(state, energy):
    df = pd.merge(state.reset_index(), energy.reset_index(), on = ['epoch', 'socket'])

    df.package *= df.state
    df.dram *= df.state

    return df.set_index(['epoch', 'id', 'tid'])[['socket', 'package', 'dram']].sort_index()

def align(vm, jiff, energy, timestamps):
    timestamps = {int(k): int(v) for k, v in timestamps.items()}

    state = align_state(vm, jiff)
    attributed = align_energy(state, energy)

    attributed = attributed.reset_index()
    attributed['timestamp'] = attributed.epoch.map(timestamps)
    attributed['timestamp'] -= attributed['timestamp'].min()
    attributed['timestamp'] += 1

    return attributed.set_index(['timestamp', 'id', 'tid'])[['socket', 'package', 'dram']]
