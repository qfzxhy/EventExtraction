import numpy as np
import seaborn as sns
import matplotlib.pyplot as plt
import codecs
import math
def load_weights():
    weights = []
    sents = []
    with codecs.open('res15_data/att_path','r','utf-8') as reader:
        for line in reader.readlines():
            units = line.strip().split()
            n = int(math.sqrt(len(units)))
            weight = np.zeros(shape=(n,n),dtype=np.float32)
            for i in range(len(units)):
                row = i // n
                col = i % n
                weight[row,col] = float(units[i])
            weights.append(weight)
    with codecs.open('res15_data/bio_res_2015_test_op','r','utf-8') as reader:
        for line in reader.readlines():
            units = eval(line.strip())
            sent = [unit[0] for unit in units]
            sents.append(sent)
    return weights,sents
def get_heat_map(sents,weights):

    sns.heatmap(weights, vmin=0, vmax=1, center=0, xticklabels=sents, yticklabels=sents)
    plt.show()

if __name__ == '__main__':
    weights, sents = load_weights()
    k = 377
    get_heat_map(sents[k],weights[k])
    k = 112
    get_heat_map(sents[k], weights[k])
    k = 189
    get_heat_map(sents[k], weights[k])