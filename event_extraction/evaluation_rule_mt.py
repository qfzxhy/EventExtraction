import codecs
def load_gold():
    Arg_entitys = []
    triggers = []
    with codecs.open('results/result_gold','r','utf-8') as reader:
        for line in reader.readlines():
            entitys,tris = [],[]
            units = line.strip().split(',')[2:]
            i = 0
            while i < len(units):
                entitys.append(units[i])
                tris.append(units[i+1])
                entitys.append(units[i+2])
                i += 3
            Arg_entitys.append(entitys)
            triggers.append(tris)
    return Arg_entitys, triggers
def load_rule_pred():
    Arg_entitys = []
    triggers = []
    with codecs.open('results/result_byrule', 'r', 'utf-8') as reader:
        for line in reader.readlines():
            entitys, tris = [], []
            sent = line.strip().split('\t')[0]
            units = line.strip().split('\t')[1:]
            if len(units) > 0:
                tris.append(units[0])
                for unit in units[1:]:
                    us = unit.split('_')
                    entitys.extend(us)
            Arg_entitys.append(entitys)
            triggers.append(tris)
    return Arg_entitys,triggers
def main():
    Arg_entitys_gold, triggers_gold = load_gold()
    Arg_entitys_pred, triggers_pred = load_rule_pred()
    precision,recall,f1score = 0.0,0.0,0.0
    num_pred,num_gold,num_right = 0,0,0
    for tri_gold,tri_pred in zip(triggers_gold,triggers_pred):
        num_pred += len(set(tri_pred))
        num_gold += len(set(tri_gold))
        count = 0
        for tri in tri_gold:
            if len(tri_pred) > 0 and tri in tri_pred[0]:
                print(tri_pred)
                print(tri_gold)
                count += 1
        num_right += count
    precision = num_right / num_pred
    recall = num_right / num_gold
    f1score = 2 * precision * recall / (precision + recall)
    print('trigger eval result %.4f,%.4f,%.4f'%(precision,recall,f1score))
    precision, recall, f1score = 0.0, 0.0, 0.0
    num_pred, num_gold, num_right = 0, 0, 0
    for arg_gold, arg_pred, tri_pred in zip(Arg_entitys_gold, Arg_entitys_pred, triggers_pred):
        num_pred += len(set(arg_pred))
        num_gold += len(set(arg_gold))
        num_right += len(set(arg_gold) & set(arg_pred))
        for arg in arg_gold:
            if len(tri_pred) > 0 and arg in tri_pred[0]:
                num_right += 1
                num_pred += 1
    precision = num_right / num_pred
    recall = num_right / num_gold
    f1score = 2 * precision * recall / (precision + recall)
    print('argument eval result %.4f,%.4f,%.4f' % (precision, recall, f1score))

if __name__ == '__main__':
    main()