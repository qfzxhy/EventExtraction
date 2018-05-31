import codecs

def get_trigger_num():
    num = 0
    sents = codecs.open('datas/newtrainCorpus', 'r', 'utf-8')

    for sent in sents:
        for unit in sent.strip().split():
            if unit.split('/')[2] == 'TRIG':
                num+=1
    print(num)


def get_adjoin_trigger_num():
    num = 0
    sents = codecs.open('datas/newtrainCorpus', 'r', 'utf-8')
    for sent in sents:

        units = sent.strip().split()
        unit_len = len(units)
        for uid,unit in enumerate(units):

            if unit.split('/')[2] == 'TRIG':

                if (uid > 0 and 'B-' in units[uid-1]) or (uid < unit_len-1 and 'B-' in units[uid+1] ):
                    num += 1
    print(num)

def get_trigger_entity_detail():
    sents = codecs.open('datas/newtrainCorpus', 'r', 'utf-8')
    labels = {'nr':0,"ns":1,"country":2,"nt":3,"dev":4,"role":5}
    dict1 = {}
    dict2 = {}
    dict3 = {}
    for sent in sents:

        units = sent.strip().split()
        unit_len = len(units)
        for uid, unit in enumerate(units):

            if unit.split('/')[2] == 'TRIG':
                trig = unit.split('/')[0]
                if trig not in dict1:
                    dict1[trig] = [0]*6
                if trig not in dict2:
                    dict2[trig] = [0]*6
                if trig not in dict3:
                    dict3[trig] = 0
                for i in range(uid-1,-1,-1):
                    if 'B-' in units[i]:
                        k = labels[units[i].split('/')[2].split('-')[1]]
                        dict1[trig][k] += 1
                        dict3[trig]+=1
                        break
                for i in range(uid+1,len(units)):
                    if 'B-' in units[i]:
                        k = labels[units[i].split('/')[2].split('-')[1]]
                        dict2[trig][k] += 1
                        dict3[trig] += 1
                        break
    print(dict1)
    print(len(dict1))
    print(dict2)
    print(len(dict2))
    print(dict3)
    dict4 = {0:0,1:0,2:0,3:0,4:0,5:0,6:0}
    for trig in dict1:
        c = 0
        for g in dict1[trig]:
            if g != 0:
                c += 1
        dict4[c] += 1
    print(dict4)
get_trigger_num()
get_adjoin_trigger_num()
get_trigger_entity_detail()