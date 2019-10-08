import os
import sys

sys.path.append('scripts/analysis')

import matplotlib.pyplot as plt
import numpy as np
import pandas as pd
import seaborn as sns

pd.options.display.float_format = '{:,.4f}'.format
pd.set_option('display.max_rows', 500)
pd.set_option('display.max_columns', 500)
pd.set_option('display.width', 1000)

from summary.method import filter_to_application

for bench in ['graphchi', 'h2']:
    ref = pd.read_csv('../chappie-data/pre-release/reference/{}.csv'.format(bench))
    ref = ref[ref.vm == 1]
    ref = ref[ref.os == 4]

    ref = ref[~ref['stack'].str.contains('chappie')]
    ref['method'] = ref['stack'].str.split(';').map(filter_to_application).str[0]

    ref = ref.groupby('method').energy.sum()
    ref = ref[ref.index != 'end']

    ref /= ref.sum()
    ref = ref.reset_index().assign(k = 'ref')

    df = pd.read_csv('../chappie-data/{}/1-4/summary/method.csv'.format(bench))
    corrs = pd.concat([df, ref], sort = False).groupby(['method', 'k']).energy.sum().unstack()
    corrs = corrs[corrs > 0].dropna(subset = ['ref']).corr()['ref']
    corrs = corrs[corrs.index != 'ref'].to_frame()
    corrs.index = [str(k).split(r'.')[0] for k in corrs.index]
    print(corrs)

    # sns.heatmap(corrs, vmin = -1, vmax = 1, annot = True)
    # plt.savefig('{}_correlation.pdf'.format(bench), bbox_inches = 'tight')
