__author__ = 'Jonatan & Baruch'

import sys

import numpy as np
import pandas as pd
from sklearn.cluster import KMeans, AgglomerativeClustering
import sklearn.cluster as cluster

import matplotlib.pyplot as plt


# http://scikit-learn.org/stable/modules/mixture.html#mixture
from sklearn.mixture import GMM, DPGMM, VBGMM

# http://scikit-learn.org/stable/modules/multiclass.html#multiclass
from sklearn.multiclass import OneVsRestClassifier, OneVsOneClassifier, OutputCodeClassifier

# http://scikit-learn.org/stable/modules/lda_qda.html#lda-qda
# from sklearn.discriminant_analysis import LinearDiscriminantAnalysis, QuadraticDiscriminantAnalysis

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

plt.figure(figsize=(12, 12))


def run_svc(X_train,y_train):
	C = 1.0
	h = .02
	svc = SVC(kernel='linear', C=C).fit(X_train,y_train)
	X = X_train
	x_min, x_max = X[:, 0].min() - 1, X[:, 0].max() + 1
	y_min, y_max = X[:, 1].min() - 1, X[:, 1].max() + 1
	xx, yy = np.meshgrid(np.arange(x_min, x_max, h),
	                     np.arange(y_min, y_max, h))

	# Plot the decision boundary. For that, we will assign a color to each
	# point in the mesh [x_min, m_max]x[y_min, y_max].
	plt.subplot(2, 2,  1)
	plt.subplots_adjust(wspace=0.4, hspace=0.4)

	Z = svc.predict(np.c_[xx.ravel(), yy.ravel()])

	# Put the result into a color plot
	Z = Z.reshape(xx.shape)
	plt.contourf(xx, yy, Z, cmap=plt.cm.Paired, alpha=0.8)

	# Plot also the training points
	plt.scatter(X[:, 0], X[:, 1], c=y, cmap=plt.cm.Paired)
	plt.xlabel('Sepal length')
	plt.ylabel('Sepal width')
	plt.xlim(xx.min(), xx.max())
	plt.ylim(yy.min(), yy.max())
	plt.xticks(())
	plt.yticks(())
	plt.title("SVC")

def run_kmeans(X_train, y_train):
    y_pred = KMeans(n_clusters=5).fit_predict(X_train)
    plt.subplot(221)
    plt.scatter(X_train[:, 2], X_train[:, 3], c=y_pred)
    plt.title("Incorrect Number of Blobs")

    plt.show()


def main():
    
    # Load the prepared training set
    train = load_from_file("train")
    X_train,y_train = prepare_prediction_data(train)
    
    # Train at least two discriminative models (including cross validation)        
    classifiers = {
	    # Clustering Algorithms
        "KMeans (2)" : KMeans(n_clusters=2, n_init=10),

		#"Linear SVC" : LinearSVC(C=1),
		# "SVC" : SVC(C=1),
		#
		# "GMM" : GMM,
		# "DPGMM" : DPGMM,
		# "VBGMM" : VBGMM,
		#
		# "LDA" : LinearDiscriminantAnalysis,
		# "QDA" : QuadraticDiscriminantAnalysis,
		#
		# "GaussianNB" : GaussianNB,


		#
		# "OneVsRestClassifier" : OneVsRestClassifier,
		# "OneVsOneClassifier" : OneVsOneClassifier,
		# "OneVsOneClassifier" : OutputCodeClassifier,
		#
		# "AgglomerativeClustering" : AgglomerativeClustering(n_clusters=5,
		# linkage='ward', connectivity=''),
		# "AffinityPropagation" : cluster.AffinityPropagation(damping=1),
		# "AgglomerativeClustering" : cluster.AgglomerativeClustering(),
		# "Birch" : cluster.Birch(threshold=1,branching_factor=1),
		# "DBSCAN" : cluster.DBSCAN(eps=1, min_samples=1, metric=1),
		# "FeatureAgglomeration" : cluster.FeatureAgglomeration(n_clusters=1),
		# "MiniBatchKMeans" : cluster.MiniBatchKMeans(n_clusters=1, init=1),
		# "MeanShift" : cluster.MeanShift(bandwidth=1, seeds=1),
		# "SpectralClustering" : cluster.SpectralClustering(n_clusters=1)

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

	#run_kmeans(X_train, y_train)

    # Gaussian Naive-Bayes with no calibration
    clf = GaussianNB()
    print X_train[:,2:4]
    #print y_train
    clf.fit(X_train[:,2:4], y_train)  # GaussianNB itself does not support sample-weights
    #print clf.class_count_
    print clf.score(X_train,y_train)

    #prob_pos_clf = clf.predict_proba(X_test)
    #print len(prob_pos_clf)

    #run_svc(X_train, y_train)
    #a = input("Wait")
# Apply the trained models on the test set and check performance


    # Select the best model for the prediction tasks
    # chosen_classifier_name = res[keys[0]]
    # chosen_classifier = classifiers[chosen_classifier_name]
    # print "The best prediction model: ", chosen_classifier_name + "\n"
    #
    # chosen_classifier.fit(X_train, y_train)


	# Incorrect number of clusters

    # Use the selected model to provide the following:
    # a. Predict to which party each person in the test set will vote
    # prediction = chosen_classifier.predict(X_test)


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
