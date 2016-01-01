__author__ = 'Jonatan & Baruch'

import sys

import numpy as np
import pandas as pd
from sklearn.cluster import KMeans, AgglomerativeClustering
import sklearn.cluster as cluster

# http://scikit-learn.org/stable/modules/mixture.html#mixture
from sklearn.mixture import GMM, DPGMM, VBGMM

# http://scikit-learn.org/stable/modules/multiclass.html#multiclass
from sklearn.multiclass import OneVsRestClassifier, OneVsOneClassifier, OutputCodeClassifier

# http://scikit-learn.org/stable/modules/lda_qda.html#lda-qda
from sklearn.discriminant_analysis import LinearDiscriminantAnalysis, QuadraticDiscriminantAnalysis

# http://scikit-learn.org/stable/modules/svm.html#svm
from sklearn.svm import SVC, LinearSVC

from sklearn.naive_bayes import GaussianNB
from sklearn.cross_validation import train_test_split
from sklearn import cross_validation



# from sklearn.ensemble import RandomForestClassifier
# from sklearn.tree import DecisionTreeClassifier
# from sklearn.linear_model import Perceptron, LinearRegression, LogisticRegression


# from sklearn.preprocessing import Imputer, StandardScaler

from itertools import combinations
from sklearn.metrics import confusion_matrix

DEFAULT_TEST_SIZE = .3

def load_from_file(name):
    df = pd.read_csv("ElectionsData-%s.csv" % name, header=0)

    return df

def prepare_prediction_data(electionsData):
    x = electionsData.drop(['Vote'], axis=1).values
    y = electionsData.Vote.values
    return x,y

# Run cross validation prediction with different classifiers
def run_prediction_with_cross_validation(x,y,classifiers,parts):
    
    results = {}    
    
    for classifier_name, classifier in classifiers.items():
        
        scores = cross_validation.cross_val_score(classifier, x, y, cv=parts)
              
        results[np.mean(scores)] = classifier_name

    return results

def main():
    
    # Load the prepared training set
    train = load_from_file("train")
    X_train,y_train = prepare_prediction_data(train)
    
    # Train at least two discriminative models (including cross validation)        
    classifiers = {
	 # Clustering Algorithms
       "KMeans (5)" : KMeans(n_clusters=5, n_init=1),
       "AgglomerativeClustering" : AgglomerativeClustering(n_clusters=5,
        linkage='ward', connectivity=''),
        "AffinityPropagation" : cluster.AffinityPropagation(damping=1),
		"AgglomerativeClustering" : cluster.AgglomerativeClustering(),
		"Birch" : cluster.Birch(threshold=1,branching_factor=1),
		"DBSCAN" : cluster.DBSCAN(eps=1, min_samples=1, metric=1),
		"FeatureAgglomeration" : cluster.FeatureAgglomeration(n_clusters=1),
		"KMeans" : cluster.KMeans(n_clusters=1, init=1, n_init=1),
		"MiniBatchKMeans" : cluster.MiniBatchKMeans(n_clusters=1, init=1),
		"MeanShift" : cluster.MeanShift(bandwidth=1, seeds=1),
		"SpectralClustering" : cluster.SpectralClustering(n_clusters=1)
     # "Linear SVM OVR" : LinearSVC(C=1)
    }
        
    res = run_prediction_with_cross_validation(X_train,y_train,classifiers,10)
    keys = res.keys()
    keys.sort()
    keys.reverse()
    print "Model scores:"
    for key in keys:
        print "%s (%.3f)" % (res[key], key)
    
    # Load the prepared test set    
    test = load_from_file("test")
    X_test,y_test = prepare_prediction_data(test)   
    
    # Apply the trained models on the test set and check performance
    
    
    # Select the best model for the prediction tasks
    chosen_classifier_name = res[keys[0]]
    chosen_classifier = classifiers[chosen_classifier_name] 
    print "The best prediction model: ", chosen_classifier_name + "\n"

    chosen_classifier.fit(X_train, y_train)
   
    # Use the selected model to provide the following:
    # a. Predict to which party each person in the test set will vote
    prediction = chosen_classifier.predict(X_test)


    # df = pd.DataFrame(data=prediction, columns=['Predicted-Vote'])
    # parties = ['Blues','Browns','Greens','Greys','Oranges','Pinks','Purples','Reds','Whites','Yellows']
    #
    # print "Parties prediction values:"
    # percentages = df.ix[:,0].value_counts(normalize=True,sort=False)
    # for p in xrange(len(parties)):
    #     print parties[p],percentages[p]
    #
    # print "The Winning party is: " + parties[int(df.ix[:,0].value_counts(normalize=True).idxmax())] + "\n"
    #
    # test['Predicted-Vote'] = df
    # df.to_csv("ElectionsData-predictvalueonly.csv",sep=',',index=False)
    # test.to_csv("ElectionsData-predict.csv",sep=',', index=False)
    #
    # # b. Construct the (test) confusion matrix and overall test error
    # print "Confusion Matrix:"
    # print confusion_matrix(y_test, prediction)
    # print
    # score = chosen_classifier.score(X_test, y_test)
    # print chosen_classifier_name + " Score: %3f" % score
    # print



main()
