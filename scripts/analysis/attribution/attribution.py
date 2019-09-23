#!/usr/bin/python3

import os
import json

from itertools import product

import pandas as pd

from attribution.processing import jvm, jiffies, energy, state

def attribute(path, status = None):
    if status:
        status.set_description('load')
    file_path = lambda f: os.path.join(path, f)
    raw = {
        file.split(r'.')[0]:
            pd.read_csv(file_path(file), delimiter = ';').sort_values('epoch')
            if 'csv' in file else
            json.load(open(file_path(file)))
        for file in os.listdir(path)
    }

    data = {}

    if status:
        status.set_description('jvm  ')
    vm = jvm.process(raw['vm'], raw['id'], raw['tid'], raw['chappie'])

    if status:
        status.set_description('jiff ')
    jiff = jiffies.process(raw['os'], raw['name'], raw['sys'])

    if status:
        status.set_description('nrg  ')
    if 'energy' in raw:
        nrg = energy.process(raw['energy'])
    else:
        # print('no energy data; creating dummy data')
        dummy_epochs = vm.reset_index().epoch.unique()
        dummy_index = pd.MultiIndex.from_product(
            [dummy_epochs, (1, 2)],
            names = ['epoch', 'socket']
        )

        nrg = pd.DataFrame(index = dummy_index)
        nrg['package'] = 1
        nrg['dram'] = 1

    if status:
        status.set_description('attr ')
    attributed = state.align(vm, jiff, nrg, raw['time']).reset_index()

    raw['id'] = {int(k): v for k, v in raw['id'].items()}
    raw['name'] = {int(k): v for k, v in raw['name'].items()}

    attributed['name'] = attributed.id.map(raw['id']).fillna(attributed.tid.map(raw['name']))
    return attributed.set_index(['timestamp', 'id', 'name'])
