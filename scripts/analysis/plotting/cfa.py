def cfa(path):
    method = pd.read_csv(os.path.join(path, 'method.csv'))

    method['method'] = method.method.str.split('.').str[-2:].str.join('.')
    method['package'] = method.method.str.split('.').str[:-2].str.join('.')
    method['class'] = method.method.str.split('.').str[-2]

    for col in ('all', 'unfiltered', 'deep'):
        df = method.groupby([col, '{}_context'.format(col)]).sum().reset_index()

        df['method'] = df[col].str.split('.').map(lambda x: x[-1] if len(x) > 1 else x[0])
        df['class'] = df[col].str.split('.').map(lambda x: x[-2] if len(x) > 1 else x[0])
        df['package'] = df[col].str.split('.').map(lambda x: '.'.join(x[:-2])  if len(x) > 1 else x[0])

        df['method_context'] = df['{}_context'.format(col)].str.split('.').map(lambda x: x[-1] if len(x) > 1 else x[0])
        df['class_context'] = df['{}_context'.format(col)].str.split('.').map(lambda x: x[-2] if len(x) > 1 else x[0])
        df['package_context'] = df['{}_context'.format(col)].str.split('.').map(lambda x: '.'.join(x[:-2])  if len(x) > 1 else x[0])
        if col != 'unfiltered':
            df = df[~(df.package.str.contains(r'java.') | df.package.str.contains(r'chappie.'))]

        for col2 in ('method', 'class', 'package'):
            for m in top[col, col2]:
                df2 = df[df[col2] == m]
                df2.energy /= df2.energy.sum()
                df2 = df2[df2.energy > 0]
                df2 = df2.groupby('{}_context'.format(col2)).energy.sum()
                # df2.index = df2['{}_context'.format(col2)]
                # df2 = df2.energy

                if len(df2) > 0:
                    df2.plot(kind = 'pie', title = m, labels = None)
                    plt.ylabel('')
                    plt.legend(df2.index)

                    plt.savefig(os.path.join(args.benchmark, 'plots', '{}_{}_{}_context.pdf'.format(m.replace('<', '').replace('>', ''), col, col2)), bbox_inches = 'tight', legend = True)

                # df2 = df[df[col2] == top[col, col2]]
                # df2 = df2.groupby([col2, '{}_context'.format(col2)]).energy.sum().sort_values()
                # print(df2.tail(10))
                # df2.tail(10).plot(kind = 'barh', width = 0.3)

            # df.tail(10).plot(kind = 'barh', width = 0.3)
            # plt.savefig(os.path.join(path, 'plots', '{}_ranking.svg'.format(col2)), bbox_inches = 'tight')
