from argparse import ArgumentParser

template = {
  "vm_options": "",
  "jar": "{jar_path}",
  "main": "{main_class}",
  "iters": 1,
  "dargs": "{dargs}",
  "args": "{args}"
}

def parse_args():
    parser = ArgumentParser()
    parser.add_argument('config')
    args = parse.parse_args()

    return args

def build_call(config):
    config.call_args.update(template)
    config.call_args = {key: arg.format(config.params) for key, arg in config.call_args.items()}
    call =
        "java {chappie_vm_options} {vm_options} "                            + \
        " -javaagent:{chappie_path}/chappie.jar "                            + \
        " -agentpath:{chappie_path}/build/liblagent.so "                     + \
        " {chappie_dargs} {app_dargs} "                                      + \
        " -jar:{chappie_path}/chappie.jar:{chappie_path}/dependencies/*.jar" + \
        ":{jar} "                                                            + \
        " {chappie_dargs} {app_dargs} "                                      + \
        " {main} {args}"

    print(call.format(call_args))
