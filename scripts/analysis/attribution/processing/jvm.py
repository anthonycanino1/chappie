import numpy as np

def process(vm, id, tid, chappie):
    id = {int(k): v for k, v in id.items()}
    tid = {int(k): int(v) for k, v in tid.items()}

    vm['tid'] = vm.id.map(tid).fillna(-1).astype(int)
    vm['name'] = vm.id.map(id).fillna("").astype(str)
    vm['os_name'] = vm.name.str[:15]

    # can filter based on actual state type at some point
    vm['activeness'] = 1
    vm_size = sum(vm.name.str.contains('chappie'))
    size_diff = vm_size - len(chappie)
    activeness = (chappie.elapsed / chappie.total).fillna(0).values[:vm_size]
    if size_diff > 0:
        activeness = np.append(activeness, np.zeros(size_diff))

    vm.loc[vm.name.str.contains('chappie'), 'activeness'] = activeness

    return vm.set_index(['epoch', 'name', 'os_name', 'id', 'tid'])[['activeness']]
