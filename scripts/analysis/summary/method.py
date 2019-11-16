import os

import numpy as np
import pandas as pd

from tqdm import tqdm

from summary.component import JVM_JAVA

def filter_to_application(trace):
    try:
        while len(trace) > 0:
            record = trace[0]
            exclude = any((
                (r'java.' in record and '.java\.' not in record),
                (r'javax.' in record and '.javax\.' not in record),
                (r'jdk.' in record and '.jdk\.' not in record),
                (r'sun.' in record and '.sun\.' not in record),
                (r'org.apache.commons.' in record and '.org.apache.commons\.' not in record),
                (r'<init>' in record)
            ))
            if not exclude:
                return trace
            else:
                trace.pop(0)
    except:
        pass

    return 'end'

def method(path):
    iters = np.sort(os.listdir(path))
    warm_up = len(iters) // 5
    df = pd.concat([pd.read_csv(os.path.join(path, f)).assign(iter = i % 8) for i, f in enumerate(tqdm(iters))])
    df['energy'] = df.package + df.dram

    mask = (df.trace == 'end') | df.trace.str.contains('chappie') | df.trace.str.contains('jlibc') | df.trace.str.contains('jrapl') | df.name.isin(JVM_JAVA)
    df = df[~mask]

    df['trace'] = df.trace.str.split(';').map(filter_to_application).str.join(';')
    df = df[(df.trace != 'end') & (df.trace != 'e;n;d')]
    corrs = df.copy('deep')
    corrs['method'] = corrs.trace.str.split(';').str[0]
    corrs = corrs.groupby(['method', 'iter']).energy.sum()

    import seaborn as sns
    import matplotlib.pyplot as plt
    corrs = corrs.unstack()
    corrs = corrs.corr()

    plt.figure(figsize = (12, 9))
    ax = sns.heatmap(corrs, vmin = 0.75, vmax = 1, annot = True, fmt = ".2f", cmap = 'Reds', annot_kws = {'fontsize': 20})

    ax.collections[0].colorbar.set_label('correlation coefficient', fontsize = 20)
    ax.collections[0].colorbar.ax.tick_params(labelsize = 16)

    plt.xlabel('OS Sampling Rate (ms)', fontsize = 20)
    plt.ylabel('VM Sampling Rate (ms)', fontsize = 20)

    plt.xticks(fontsize = 24)
    plt.yticks(fontsize = 24)

    plt.savefig('{}_autocorr.pdf'.format(path.split('/')[-3]), bbox_inches = 'tight')
    plt.close()

    df = df.groupby('trace').energy.agg(('sum', 'count'))
    df.columns = ['energy', 'time']

    return df
