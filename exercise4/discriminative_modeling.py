__author__ = 'Jonatan & Baruch'

import sys

import numpy as np
import pandas as pd
import copy
from sklearn.naive_bayes import GaussianNB
from sklearn.cluster import KMeans
from sklearn.neighbors import KNeighborsClassifier
from sklearn.cross_validation import train_test_split
from sklearn import cross_validation
from sklearn.ensemble import RandomForestClassifier, ExtraTreesClassifier, GradientBoostingRegressor
from sklearn.ensemble import BaggingClassifier, AdaBoostClassifier, GradientBoostingClassifier
from sklearn.tree import DecisionTreeClassifier
from sklearn.linear_model import Perceptron, LinearRegression, LogisticRegression

from sklearn.cross_validation import StratifiedShuffleSplit
from sklearn.grid_search import GridSearchCV

from sklearn.svm import SVC, LinearSVC
from sklearn.preprocessing import Imputer, StandardScaler

from itertools import combinations
from sklearn.metrics import confusion_matrix
from sklearn.base import BaseEstimator
from sklearn.base import ClassifierMixin
from sklearn.base import TransformerMixin
from sklearn.preprocessing import LabelEncoder
from sklearn.externals import six
from sklearn.base import clone
from sklearn.pipeline import _name_estimators

import matplotlib.pyplot as plt
from mpl_toolkits.mplot3d import Axes3D

VOTES = {0:'Blues', 1:'Browns', 2:'Greens', 3:'Greys', 4:'Oranges', 5:'Pinks', 6:'Purples', 7:'Reds', 8:'Whites', 9:'Yellows'}
PARTIES = map(lambda x: x[1], sorted(VOTES.items()))
CV_PARTS = 10
GROUPS = ([1,6], [2,5,8],[3,7],[0,4,9])
DEFAULT_TEST_SIZE = .3

def load_from_file(name):
    df = pd.read_csv("ElectionsData-%s.csv" % name, header=0)

    return df

def prepare_prediction_data(electionsData):
    x = electionsData.drop(['Vote'], axis=1).values
    y = electionsData.Vote.values
    return x,y

def prepare_joint_prediction_data(electionsData):
    electionsData['XVOTE'] = [1 if (x in GROUPS[0]) else 2 if (x in GROUPS[1]) else 3 if (x in GROUPS[2]) else 4 for x in electionsData['Vote']]
    x = electionsData.drop(['XVOTE'], axis=1).drop(['Vote'], axis=1).values
    y = electionsData.XVOTE.values
    return x,y

def prepare_partial_prediction_data(electionsData, parties,SVC=False):
    electionsData = electionsData[electionsData['Vote'].isin(parties)]

    if (SVC):
        x = electionsData[['Overall_happiness_score','Yearly_ExpensesK', 'Yearly_IncomeK']].values
    else:
        x = electionsData.drop(['Vote'], axis=1).values
    y = electionsData.Vote.values
    return x,y, len(electionsData.index)

# Run cross validation prediction with different classifiers
def run_prediction_with_cross_validation(x,y,x_svc,y_svc,classifiers,parts):

    results = {}

    #test = load_from_file("validation")
    #X_test,y_test = prepare_joint_prediction_data(test)
    for classifier_name, classifier in classifiers.items():
        if (isinstance(classifier,SVC) and x_svc != []):
            scores = cross_validation.cross_val_score(classifier, x_svc, y_svc, cv=parts)
        else:
            scores = cross_validation.cross_val_score(classifier, x, y, cv=parts)
        results[np.mean(scores)] = classifier_name
        #clf = classifier.fit(x, y)
        #print "Test Score " + classifier_name + " : " + str(clf.score(X_test, y_test))

        #test['Predicted-Vote'] = clf.predict(X_test)

    return results

def find_best_prediction_classifier(x,y,classifiers,parts,x_svc=[],y_svc=[]):
    res = run_prediction_with_cross_validation(x,y,x_svc,y_svc,classifiers,parts)

    keys = res.keys()
    keys.sort()
    keys.reverse()
    print "Model scores:"
    for key in keys[:2]:
        print "%s (%.3f)" % (res[key], key)
    return res[keys[0]],keys[0]

def find_classifiers():
    classifiers = {
    #"GradientBoostingClassifier" : GradientBoostingClassifier(n_estimators=100, learning_rate=1.0, max_depth=1, random_state=0),
#    "GradientBoostingRegressor" : GradientBoostingRegressor(n_estimators=100, learning_rate=0.1, max_depth=11, random_state=0, loss='ls'),
#    "Decision Tree(10)-entropy" : DecisionTreeClassifier(max_depth=10, criterion="entropy"),
#    "Decision Tree(11)-entropy" : DecisionTreeClassifier(max_depth=11, criterion="entropy"),
#    "Decision Tree(12)-entropy" : DecisionTreeClassifier(max_depth=12, criterion="entropy"),

#    "Decision Tree(13)-entropy" : DecisionTreeClassifier(max_depth=13, criterion="entropy"),
#
#    "Decision Tree(10)" : DecisionTreeClassifier(max_depth=10), # Gini is default
#    "Decision Tree(12)" : DecisionTreeClassifier(max_depth=12), # Gini is default
#    "Decision Tree(13)" : DecisionTreeClassifier(max_depth=13), # Gini is default
#    "Decision Tree(11)" : DecisionTreeClassifier(max_depth=11), # Gini is default
#
#	"Random Forest (8)" : RandomForestClassifier(max_depth=8, n_estimators=20, max_features=5),
#	"Random Forest (9)" : RandomForestClassifier(max_depth=9, n_estimators=25, max_features=5),
#    "Random Forest (10)" : RandomForestClassifier(max_depth=10, n_estimators=25, max_features=5),

    "Random Forest (11)" : RandomForestClassifier(max_depth=11, n_estimators=25, max_features=5),

#    "Random Forest (12)" : RandomForestClassifier(max_depth=12, n_estimators=20, max_features=5),
#
#    "BaggingClassifier(Random Forest11)" : BaggingClassifier(RandomForestClassifier(max_depth=11, n_estimators=25, max_features=5)),
#

    "ExtraTreesClassifier (11)" : ExtraTreesClassifier(max_depth=11, n_estimators=25, max_features=5),

#    "ExtraTreesClassifier (7)" : ExtraTreesClassifier(max_depth=7, n_estimators=25, max_features=5),
#    "ExtraTreesClassifier (11-50estimators)" : ExtraTreesClassifier(max_depth=11, n_estimators=50, max_features=5),
#    "ExtraTreesClassifier (11-8features)" : ExtraTreesClassifier(max_depth=11, n_estimators=25, max_features=8),
#
#
    "BaggingClassifier(ExtraTrees11)" : BaggingClassifier(RandomForestClassifier(max_depth=11, n_estimators=25, max_features=5)),
#

#     "LinearRegression" :  LinearRegression(),
#     "LogisticRegression" :  LogisticRegression(),
#
#     "Nearest Neighbors(4)" : KNeighborsClassifier(4),
      #"Nearest Neighbors(8)" : KNeighborsClassifier(8),
#      "Nearest Neighbors(7)" : KNeighborsClassifier(7),
#

#      "Nearest Neighbors(8)-distance" : KNeighborsClassifier(8, weights='distance'),
#     "Nearest Neighbors(9)-distance" : KNeighborsClassifier(9, weights='distance'),
#      "Nearest Neighbors(7)-distance" : KNeighborsClassifier(7, weights='distance'),
#      "Nearest Neighbors(6)-distance" : KNeighborsClassifier(6, weights='distance'),
#      "Nearest Neighbors(5)-distance" : KNeighborsClassifier(5, weights='distance'),
#      "Nearest Neighbors(4)-distance" : KNeighborsClassifier(4, weights='distance'),

#
#     "Naive Bayes" : GaussianNB(),
#
#     "Perceptron" : Perceptron(n_iter=50),
#
    "Linear SVM linear" : SVC(kernel="linear", C=100),
    "Linear SVM rbf" : SVC(kernel="rbf", C=100),
    #"Linear SVM rbf g=0.6" : SVC(gamma=0.6, kernel="rbf", C=100),
    #"Linear SVM rbf g=0.4" : SVC(gamma=0.4, kernel="rbf", C=100),
    #"Linear SVM rbf 200 g=0.4" : SVC(gamma=0.4,kernel="rbf", C=200),
#
#     "Linear SVM OVR" : LinearSVC(C=1),

#
#    "SVC 0.025" : SVC(kernel="linear", C=0.025),
#    "SVC 1" : SVC(kernel="rbf", gamma=1e-1, C=1e-2),
#    "SVC 2" : SVC(kernel="rbf", gamma=1, C=1e-2),
#"SVC 3" : SVC(kernel="rbf", gamma=1e1, C=1e-2),
#"SVC 4" : SVC(kernel="rbf", gamma=1e-1, C=1),
#"SVC 5" : SVC(kernel="rbf", gamma=1, C=1),
#"SVC 6" : SVC(kernel="rbf", gamma=1e1, C=1),
#"SVC 7" : SVC(kernel="rbf", gamma=1e-1, C=1e2),
#     "SVC 8 g=0.05" : SVC(kernel="rbf", gamma=0.05, C=1e2),

#"SVC 8 g=0.35 2" : SVC(kernel="rbf", gamma=0.35, C=1e2),

#"SVC 8 g=0.35 3" : SVC(kernel="rbf", gamma=0.35, C=1e3),
#"SVC 8 g=-0.35 3" : SVC(kernel="rbf", gamma=-0.35, C=1e3),
#"SVC 8 g=0.35 4" : SVC(kernel="rbf", gamma=0.35, C=1e4),
#"SVC 8 g=0.35 5" : SVC(kernel="rbf", gamma=0.35, C=1e5),

#"SVC 8 g=1.5" : SVC(kernel="rbf", gamma=1.5, C=1e2),
#"SVC 8 gamma=10" : SVC(kernel="rbf", gamma=10, C=1e2),
#"SVC 9" : SVC(kernel="rbf", gamma=1e1, C=1e2),

#    "SVC rbf gamma" : SVC(kernel="rbf", gamma=2, C=1),
#    "SVC rbf gamma 2" : SVC(kernel="rbf", gamma=2, C=2),
#    "SVC gamma" : SVC(gamma=2, C=1),
#    "SVC si" : SVC(kernel="sigmoid", coef0=0.0, C=0.01),

#     "AdaBoost" : AdaBoostClassifier()
#   "LDA" : LinearDiscriminantAnalysis(),
#    "QDA" : QuadraticDiscriminantAnalysis()
    }
    train = load_from_file("train")

    print "Seperating groups:"
    X_train,y_train = prepare_joint_prediction_data(train)
    classifier_name,score = find_best_prediction_classifier(X_train,y_train,classifiers,CV_PARTS)
    print X_train.shape, classifier_name
    train = load_from_file("train")
    X_train,y_train = prepare_joint_prediction_data(train)

    seperating_classifier = copy.copy(classifiers[classifier_name]).fit(X_train, y_train)

    validation = load_from_file("validation")
    X_validation,y_validation = prepare_joint_prediction_data(validation)

    print "Seperating parties:"

    scores = []
    sizes = []
    best_classifiers = []
    for l in xrange(len(GROUPS)):
       print GROUPS[l]
       X_train,y_train,size = prepare_partial_prediction_data(train, GROUPS[l])
       X_SVC_train,y_SVC_train,size = prepare_partial_prediction_data(train, GROUPS[l],True)
       classifier_name, classifier_score = find_best_prediction_classifier(X_train,y_train, classifiers,CV_PARTS, X_SVC_train,y_SVC_train)

       best_classifiers.append(copy.copy(classifiers[classifier_name]).fit(X_train,y_train))
       sizes.append(size)
       scores.append(classifier_score)

    calculate_overall_score(scores,sizes)
    return seperating_classifier,best_classifiers

def calculate_overall_score(scores, data_sizes):
    overall_score = 0
    total = sum(data_sizes)*1.0
    for i in xrange(len(scores)):
	    overall_score += scores[i]*(data_sizes[i]/total)
    print "Overall: ", overall_score


def predict_elections(seperating_classifier,group_classifiers):
    VALIDATION_FILE = "Pred_Features-pred"

    validation = load_from_file(VALIDATION_FILE)
    data = validation.drop(['IdentityCard_Num'],axis=1).values

    validation['XVOTE'] = seperating_classifier.predict(data)

    predict = []

    for i in xrange(len(group_classifiers)):
        validation_sub_group = validation[validation['XVOTE'] == (i+1)]
        validation_sub_group ['Predicted-Vote'] = group_classifiers[i].predict(validation_sub_group.drop(['IdentityCard_Num'],axis=1).values)
        predict.append(validation_sub_group)

    test = pd.concat(predict,axis=0)

    return test


def validate_classification(seperating_classifier,group_classifiers, VALIDATION_FILE="validation"):

    # Load the prepared test set

    # validation = load_from_file(VALIDATION_FILE)
    # X_validation,y_validation = prepare_joint_prediction_data(validation)
    # print "Separation Score: " + str(seperating_classifier.score(X_validation, y_validation))

    validation = load_from_file(VALIDATION_FILE)
    data,label= prepare_prediction_data(validation)
    prediction = seperating_classifier.predict(data)

    df = pd.DataFrame(data=prediction, columns=['XVOTE'])

    validation = load_from_file(VALIDATION_FILE)
    predict = []
    validation['XVOTE'] = df

    for i in xrange(len(group_classifiers)):
        #X_train,y_train,size = prepare_partial_prediction_data(train, GROUPS[i])
        validation_sub_group = validation[validation['XVOTE'] == (i+1)]

        data,label = prepare_prediction_data(validation_sub_group )

        clf = group_classifiers[i]
        score = clf.score(data,label)

        print GROUPS[i],score
        validation_sub_group['Predicted-Vote'] = clf.predict(data)

        predict.append(validation_sub_group)

    test = pd.concat(predict,axis=0)

    print "Validation Score: " + str(len(test[test['Vote'] == test['Predicted-Vote']])/(len(test)*1.0))
    return test

def summarize_prediction(test):

    print VOTES[test['Predicted-Vote'].value_counts().idxmax()]
    #print VOTES[test['Vote'].value_counts().idxmax()]

    percentages = test['Predicted-Vote'].value_counts(normalize=True,sort=False)
    print percentages
    for p in xrange(len(PARTIES)):
        print PARTIES[p],percentages[p]
    #print "Parties prediction values:"


    #print "The Winning party is: " + parties[int(df.ix[:,0].value_counts(normalize=True).idxmax())] + "\n"


def plot_3d(train,relevant_parties=PARTIES):
	fig = plt.figure(figsize=(8,8))
	ax = fig.add_subplot(111, projection='3d')

	for i, name in zip(range(len(PARTIES)), PARTIES):
		if i in relevant_parties:

			cur = train[train['Predicted-Vote']==i]
			ax.scatter(cur.Overall_happiness_score, cur.Avg_Residancy_Altitude, cur.Yearly_IncomeK, label=name, color=name[:-1])
			#ax.scatter(cur.Overall_happiness_score, cur.Yearly_ExpensesK, cur.Yearly_IncomeK, label=name, color=name[:-1])


	ax.view_init(elev=65, azim=-71)

	plt.legend(loc='best')
	plt.show()




def add_bin_plot(n_clusters,column, y_train, plot_spot,title):
	clf = KMeans(n_clusters=n_clusters)
	y_pred = clf.fit(column)
	values = y_pred.cluster_centers_.squeeze()
	labels = y_pred.labels_
	comp = np.choose(labels,values)

	plt.subplot(plot_spot)
	plt.scatter(comp, y_train, c=y_train)
	plt.title(title)


def plot_bins(X_train, y_train):

	n_clusters = 4

	add_bin_plot(n_clusters,X_train[:, 0:1], y_train,331,"Yearly_Expense")
	add_bin_plot(n_clusters,X_train[:, 1:2], y_train,332,"Yearly_Income")
	add_bin_plot(n_clusters,X_train[:, 2:3], y_train,333,"Overall_Happiness_Score")

	plt.subplot(334)
	plt.scatter(X_train[:, 3], y_train, c=y_train)
	plt.title("Most_Important_Issue")

	add_bin_plot(n_clusters,X_train[:, 5:6], y_train,335,"Avg_Residancy_Alt")

	plt.subplot(336)
	plt.scatter(X_train[:, 5], y_train, c=y_train)
	plt.title("Will_only_vote_large_party")

	plt.subplot(337)
	plt.scatter(X_train[:, 6], y_train, c=y_train)
	plt.title("Financial_Agenda_matters")

	plt.show()



def main():

    seperating_classifier,group_classifiers = find_classifiers()

    prediction = validate_classification(seperating_classifier,group_classifiers)
    #prediction = predict_elections(seperating_classifier,group_classifiers)

    summarize_prediction(prediction)

    return

    plot_3d(prediction,[4,5,6,7,9])
    plot_3d(prediction,[0,7,9])
    plot_3d(prediction,[1,2,3,4])
    plot_3d(prediction,[5,6,8])
    plot_bins(prediction.drop(['Predicted-Vote'],axis=1).values, prediction['Predicted-Vote'].values)



main()
