import codecs
import numpy as np
def load_copule_file(couple_fname):
    couple_infos = []
    with codecs.open(couple_fname, 'r', 'utf-8') as reader:
        for uline in reader.readlines():
            uline = uline.strip()
            cs = uline.split('\t')[1:]
            couple_infos.append(cs)
    return couple_infos
def get_couple_data(bio_data,couple_info):
    couple_tmp = np.zeros(shape=(len(bio_data), len(bio_data)),dtype=np.int32)
    for c in couple_info:
        c = c.lower()
        tar_words = c[1:c.index(',')].split()

        opi_words = c[c.index(',')+1:-1].split()

        for i in range(len(bio_data)):
            if bio_data[i][0] not in tar_words:
                continue
            for j in range(len(bio_data)):
                if bio_data[j][0] in opi_words:
                    couple_tmp[i,j] = 1
                    couple_tmp[j, i] = 1
    print(couple_tmp)
    return couple_tmp
if __name__ == '__main__':
    # fname = 'res15_data/bio_res_2015_train_op'
    # couple_fname = "res15_data/couple_data_train"
    # bio_source_datas = []
    # max_token_len = 0
    # couples = []
    # with codecs.open(fname, 'r', 'utf-8') as reader:
    #     for uline in reader.readlines():
    #         uline = eval(uline.strip())
    #         bio_source_datas.append(uline)
    # couple_infos = load_copule_file(couple_fname)
    # couples.append(get_couple_data(bio_source_datas[5],couple_infos[5]))
    # couples.append(get_couple_data(bio_source_datas[2], couple_infos[2]))
    # print(np.array(couples))
    print(np.log(1))
    # a = np.zeros(shape=(2,3,3))
    # a[0,1,2] = 1
    # print(a)
    # print(np.transpose(a,axes=(0,2,1)))