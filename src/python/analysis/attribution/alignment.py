import pandas as pd

def smooth_energy_trace(df):
    id = df.index.unique()[0]
    idx = pd.RangeIndex(df.timestamp.min(), df.timestamp.max())
    df = df.set_index('timestamp').reindex(idx)
    df.loc[df.package == df.package, 'tag'] = df.index[df.package == df.package]
    df = df.bfill()

    # get size of each group
    tag_size = df.groupby('tag').count()

    # smooth data by division with group size
    df = pd.merge(df, tag_size, on = 'tag', right_index = True, how = 'outer', suffixes = ('', '_'))
    df.package /= df.package_
    df.dram /= df.dram_

    if not df.empty:
        df = df.reset_index()
        df['timestamp'] = df['index']
        df['id'] = id
    else:
        df['timestamp'] = None

    return df.set_index('timestamp')[['package', 'dram']]

def align_methods(attributed, method):
    attributed = attributed.reset_index().dropna(subset = ['timestamp']).groupby('id').apply(smooth_energy_trace)
    attributed = attributed.reset_index().set_index(['timestamp', 'id']).sort_index()


    method = method['trace']

    trace = pd.concat([attributed, method], axis = 1)
    return trace

def fill_methods(df, limit = None):
    trace_map = df.set_index('timestamp').to_dict()['trace']

    if limit != 0:
        df.loc[df.trace == df.trace, 'tag'] = df.timestamp[df.trace == df.trace]

        df['next'] = df.tag.bfill(limit = limit)
        df['prev'] = df.tag.ffill(limit = limit)

        df = df.dropna(subset = ['next', 'prev'], how = 'all')

        next_dist = abs(df.next - df.index)
        prev_dist = abs(df.prev - df.index)
        mask = ((next_dist == next_dist) & (prev_dist != prev_dist)) | (next_dist < prev_dist)

        df.loc[mask, 'timestamp'] = df[mask].next
        df.loc[~mask, 'timestamp'] = df[~mask].prev
    else:
        df = df.dropna()

    df = df.groupby('timestamp')[['package', 'dram']].sum().reset_index()
    df.timestamp = df.timestamp.astype(int)
    df['trace'] = df.timestamp.map(trace_map)

    return df.set_index('timestamp')[['trace', 'package', 'dram']]

def align(attributed, method, id, limit = None, status = None):
    if status:
        status.set_description('align {}'.format(limit if limit is not None else 'inf'))
    aligned = align_methods(attributed, method)

    if status:
        status.set_description('smooth {}'.format(limit if limit is not None else 'inf'))
    aligned = aligned.reset_index().groupby('id').apply(fill_methods, (limit)).reset_index()
    id = {int(k): v for k, v in id.items()}
    aligned['name'] = aligned.id.map(id)

    return aligned.set_index(['timestamp', 'id', 'name'])
