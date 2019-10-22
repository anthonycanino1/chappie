import os
import shutil
import sys

import matplotlib.pyplot as plt
import numpy as np
import pandas as pd
import seaborn as sns

from tqdm import tqdm

pd.options.display.float_format = '{:.4f}'.format

pd.set_option('display.max_rows', 500)
pd.set_option('display.max_columns', 500)
pd.set_option('display.width', 1000)

root = '../chappie-data/baseline/graphchi'
if not os.path.exists(root + '/plots'):
    os.mkdir(root + '/plots')

cases = ['1-4', '1-8', '1-16', '1-32', '2-4', '2-8', '2-16', '2-32', '4-4', '4-8', '4-16', '4-32', '8-4', '8-8', '8-16', '8-32']
# np.sort(os.listdir(root)) # ['1', '100', '250', '500', '1000']
# print(cases)
# a

summary = pd.DataFrame(index = cases)
summary.index.name = 'cases'

vals = []
mthds = []
corrs = []

for i in tqdm(cases):
    if not os.path.exists(root + '/plots/' + i):
        os.mkdir(root + '/plots/' + i)

    for type in ['class', 'method', 'package']:
        shutil.copyfile('{}/{}/plots/k=4_{}_ranking.pdf'.format(root, i, type), '{}/plots/{}/k=4_{}_ranking.pdf'.format(root, i, type))

    vals.append(np.mean([pd.read_csv('{}/{}/raw/{}/chappie.csv'.format(root, i, k), delimiter = ';').total.sum() for k in range(2, 5)]) / 10**9)
    df = pd.read_csv('{}/{}/raw/method.csv'.format(root, i), header = None)
    df.columns = ['', 'timestamp', 'id', 'trace']
    df = df.drop_duplicates(['timestamp', 'id', 'trace'])

    df.timestamp = df.timestamp // 4
    df = df.groupby('timestamp').trace.count().reindex(pd.RangeIndex(df.timestamp.min(), df.timestamp.max(), 1)).fillna(0)
    mthds.append(df)

    corrs.append(pd.read_csv('{}/{}/summary/method.csv'.format(root, i)).assign(case = i))

    # if i == cases[0]:
    #     df1 = df
    # else:
    #     corrs = pd.concat([df, df1], sort = False).groupby(['method', 'k', 'type'])[['energy']].sum()
    #     corrs = corrs[corrs > 0]
    #
    #     corrs = corrs.unstack().unstack().corr()
    #     corrs = corrs.loc[('energy', i), ('energy', cases[0])]
    #     corrs.index = [int(idx) if idx != np.inf else 'inf' for idx in corrs.index]
    #     corrs.columns = [int(col) if col != np.inf else 'inf' for col in corrs.columns]
    #     print(corrs)
    #
    #     sns.heatmap(corrs, vmin = -1, vmax = 1, annot = True, fmt = '.4f')
    #
    #     plt.xlabel(cases[0])
    #     plt.ylabel(i)
    #
    #     plt.savefig(root + '/plots/{}_corr.pdf'.format(i))
    #     plt.close()

corrs = pd.concat(corrs, sort = False)
corrs = corrs[(corrs.energy > 0) & (corrs.k == 4)].dropna().groupby(['method', 'case'])[['energy']].sum().unstack().corr().iloc[2, :].reset_index()
corrs.columns = corrs.columns.droplevel(1)
corrs['vm'] = corrs.case.str.split('-').str[0].astype(int)
corrs['os'] = corrs.case.str.split('-').str[-1].astype(int)
corrs = corrs.sort_index(axis = 1)
corrs = corrs.pivot(index = 'vm', columns = 'os', values = 'energy')

print(corrs)

sns.heatmap(corrs, vmin = -1, vmax = 1, annot = True, fmt = '.4f')

plt.xlabel('os')
plt.ylabel('vm')

plt.savefig(root + '/plots/correlation.pdf'.format(i))
plt.close()

summary['runtime (s)'] = np.round(vals, 4)
summary['overhead (%)'] = np.round((vals - vals[0]) / vals[0], 4) * 100
summary['methods (#)'] = [x.sum() for x in mthds]
summary['methods (#)'] = summary['methods (#)'].astype(int)
summary['methods / call'] = [x.mean() for x in mthds]

print(summary)

fig, ax = plt.subplots()
fig.patch.set_visible(False)
ax.axis('off')
ax.axis('tight')

plt.table(rowLabels = summary.index, colLabels = summary.columns, cellText = summary.values, loc = 'center')
plt.savefig(root + '/plots/summary.pdf')
