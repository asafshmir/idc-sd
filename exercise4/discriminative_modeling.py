__author__ = 'Jonatan & Baruch'

import sys

import numpy as np
import pandas as pd
from sklearn.naive_bayes import GaussianNB
from sklearn.neighbors import KNeighborsClassifier
from sklearn.cross_validation import train_test_split
from sklearn import cross_validation
from sklearn.ensemble import RandomForestClassifier, ExtraTreesClassifier, GradientBoostingRegressor
from sklearn.ensemble import BaggingClassifier, AdaBoostClassifier, GradientBoostingClassifier
from sklearn.tree import DecisionTreeClassifier
from sklearn.linear_model import Perceptron, LinearRegression, LogisticRegression
#from sklearn.discriminant_analysis import LinearDiscriminantAnalysis
#from sklearn.discriminant_analysis import QuadraticDiscriminantAnalysis

from sklearn.cross_validation import StratifiedShuffleSplit
from sklearn.grid_search import GridSearchCV

from sklearn.svm import SVC, LinearSVC
from sklearn.preprocessing import Imputer, StandardScaler

from itertools import combinations
from sklearn.metrics import confusion_matrix

###############################################################################

# http://sebastianraschka.com/Articles/2014_ensemble_classifier.html#EnsembleClassifier---Tuning-Weights


from sklearn.base import BaseEstimator
from sklearn.base import ClassifierMixin
from sklearn.base import TransformerMixin
from sklearn.preprocessing import LabelEncoder
from sklearn.externals import six
from sklearn.base import clone
from sklearn.pipeline import _name_estimators
import operator

class EnsembleClassifier(BaseEstimator, ClassifierMixin, TransformerMixin):

    def __init__(self, clfs, voting='hard', weights=None):

        self.clfs = clfs
        self.named_clfs = {key:value for key,value in _name_estimators(clfs)}
        self.voting = voting
        self.weights = weights


    def fit(self, X, y):
        """ Fit the clfs.

        Parameters
        ----------
        X : {array-like, sparse matrix}, shape = [n_samples, n_features]
            Training vectors, where n_samples is the number of samples and
            n_features is the number of features.

        y : array-like, shape = [n_samples]
            Target values.

        Returns
        -------
        self : object
        """
        if isinstance(y, np.ndarray) and len(y.shape) > 1 and y.shape[1] > 1:
            raise NotImplementedError('Multilabel and multi-output'\
                                      ' classification is not supported.')

        if self.voting not in ('soft', 'hard'):
            raise ValueError("Voting must be 'soft' or 'hard'; got (voting=%r)"
                             % voting)

        if self.weights and len(self.weights) != len(self.clfs):
            raise ValueError('Number of classifiers and weights must be equal'
                             '; got %d weights, %d clfs'
                             % (len(self.weights), len(self.clfs)))

        self.le_ = LabelEncoder()
        self.le_.fit(y)
        self.classes_ = self.le_.classes_
        self.clfs_ = []
        for clf in self.clfs:
            fitted_clf = clone(clf).fit(X, self.le_.transform(y))
            self.clfs_.append(fitted_clf)
        return self

    def predict(self, X):
        """ Predict class labels for X.

        Parameters
        ----------
        X : {array-like, sparse matrix}, shape = [n_samples, n_features]
            Training vectors, where n_samples is the number of samples and
            n_features is the number of features.

        Returns
        ----------
        maj : array-like, shape = [n_samples]
            Predicted class labels.
        """
        if self.voting == 'soft':

            maj = np.argmax(self.predict_proba(X), axis=1)

        else:  # 'hard' voting
            predictions = self._predict(X)

            maj = np.apply_along_axis(
                                      lambda x:
                                      np.argmax(np.bincount(x,
                                                weights=self.weights)),
                                      axis=1,
                                      arr=predictions)

        maj = self.le_.inverse_transform(maj)
        return maj

    def predict_proba(self, X):
        """ Predict class probabilities for X.

        Parameters
        ----------
        X : {array-like, sparse matrix}, shape = [n_samples, n_features]
            Training vectors, where n_samples is the number of samples and
            n_features is the number of features.

        Returns
        ----------
        avg : array-like, shape = [n_samples, n_classes]
            Weighted average probability for each class per sample.
        """
        avg = np.average(self._predict_probas(X), axis=0, weights=self.weights)
        return avg

    def transform(self, X):
        """ Return class labels or probabilities for X for each estimator.

        Parameters
        ----------
        X : {array-like, sparse matrix}, shape = [n_samples, n_features]
            Training vectors, where n_samples is the number of samples and
            n_features is the number of features.

        Returns
        -------
        If `voting='soft'`:
          array-like = [n_classifiers, n_samples, n_classes]
            Class probabilties calculated by each classifier.
        If `voting='hard'`:
          array-like = [n_classifiers, n_samples]
            Class labels predicted by each classifier.
        """
        if self.voting == 'soft':
            return self._predict_probas(X)
        else:
            return self._predict(X)

    def get_params(self, deep=True):
        """ Return estimator parameter names for GridSearch support"""
        if not deep:
            return super(EnsembleClassifier, self).get_params(deep=False)
        else:
            out = self.named_clfs.copy()
            for name, step in six.iteritems(self.named_clfs):
                for key, value in six.iteritems(step.get_params(deep=True)):
                    out['%s__%s' % (name, key)] = value
            return out

    def _predict(self, X):
        """ Collect results from clf.predict calls. """
        return np.asarray([clf.predict(X) for clf in self.clfs_]).T

    def _predict_probas(self, X):
        """ Collect results from clf.predict calls. """
        return np.asarray([clf.predict_proba(X) for clf in self.clfs_])


###############################################################################

DEFAULT_TEST_SIZE = .3

def load_from_file(name):
    df = pd.read_csv("ElectionsData-%s.csv" % name, header=0)

    return df

def prepare_prediction_data(electionsData):
    x = electionsData.drop(['Vote'], axis=1).values
    y = electionsData.Vote.values
    return x,y

def prepare_joint_prediction_data(electionsData):
    electionsData['XVOTE'] = [1 if (x == 1 or x == 6) else 2 if (x ==3 or x == 7) else 3 if (x == 0 or x==4 or x == 9) else 4 for x in electionsData['Vote']]
    x = electionsData.drop(['XVOTE'], axis=1).drop(['Vote'], axis=1).values
    y = electionsData.XVOTE.values
    electionsData = electionsData.drop(['XVOTE'], axis=1).values
    return x,y

def prepare_partial_prediction_data(electionsData, parties):
    electionsData = electionsData[electionsData['Vote'].isin(parties)]
    print len(electionsData.index)
    x = electionsData.drop(['Vote'], axis=1).values
    y = electionsData.Vote.values
    return x,y, len(electionsData.index)

# Run cross validation prediction with different classifiers
def run_prediction_with_cross_validation(x,y,classifiers,parts):

    results = {}

    for classifier_name, classifier in classifiers.items():

        scores = cross_validation.cross_val_score(classifier, x, y, cv=parts)

        results[np.mean(scores)] = classifier_name

    return results

def find_best_prediction_classifier(x,y,classifiers,parts):
    res = run_prediction_with_cross_validation(x,y,classifiers,parts)
    keys = res.keys()
    keys.sort()
    keys.reverse()
    print "Model scores:"
    for key in keys:
        print "%s (%.3f)" % (res[key], key)
	return res[keys[0]],key

def main():

    # Load the prepared training set
    train = load_from_file("train")

    ec = EnsembleClassifier(clfs=[SVC(kernel="rbf", gamma=0.35, C=1e3),  GradientBoostingClassifier(n_estimators=100, learning_rate=1.0, max_depth=1, random_state=0), RandomForestClassifier(max_depth=11, n_estimators=25, max_features=5)], voting='hard')
    ec2 = EnsembleClassifier(clfs=[RandomForestClassifier(max_depth=11, n_estimators=25, max_features=5), KNeighborsClassifier(8), GradientBoostingClassifier(n_estimators=100, learning_rate=1.0, max_depth=1, random_state=0)], voting='soft')

    # Train at least two discriminative models (including cross validation)
    classifiers = {
    # "Ensemble" : ec,
    # "Ensemble2" : ec2,
    # "GradientBoostingClassifier" : GradientBoostingClassifier(n_estimators=100, learning_rate=1.0, max_depth=1, random_state=0),
#    "GradientBoostingRegressor" : GradientBoostingRegressor(n_estimators=100, learning_rate=0.1, max_depth=11, random_state=0, loss='ls'),
    "Decision Tree(10)-entropy" : DecisionTreeClassifier(max_depth=10, criterion="entropy"),
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
#     "Random Forest (11) e" : RandomForestClassifier(max_depth=11, n_estimators=25, max_features=5, criterion="entropy"),
     "Random Forest (11) 6" : RandomForestClassifier(max_depth=11, n_estimators=25, max_features=6),
#     "Random Forest (11) 7" : RandomForestClassifier(max_depth=11, n_estimators=25, max_features=7),
 #    "Random Forest (11) 4" : RandomForestClassifier(max_depth=11, n_estimators=25, max_features=4),
  #   "Random Forest (11) n50" : RandomForestClassifier(max_depth=11, n_estimators=50, max_features=5),
  #   "Random Forest (11) n5" : RandomForestClassifier(max_depth=11, n_estimators=5, max_features=5)
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
#    "BaggingClassifier(ExtraTrees11)" : BaggingClassifier(RandomForestClassifier(max_depth=11, n_estimators=25, max_features=5)),
#
     "LinearRegression" :  LinearRegression(),
#     "LogisticRegression" :  LogisticRegression(C=10),
#
     "Nearest Neighbors(4)" : KNeighborsClassifier(4),
#       "Nearest Neighbors(8)" : KNeighborsClassifier(8),
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
     # "Perceptron" : Perceptron(n_iter=50, penalty='elasticnet',alpha=10),
#
      "Linear SVM OVO" : SVC(kernel="linear", C=1),
#
     "Linear SVM OVR" : LinearSVC(C=1)
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
# "SVC 8 g=0.35 2" : SVC(kernel="poly", gamma=2,degree=5)
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


    print "Seperating groups:"
    X_train,y_train = prepare_joint_prediction_data(train)
    separator = find_best_prediction_classifier(X_train,y_train,classifiers,10)



    print "Seperating parties:"
    groups = ([1,6], [3,7], [0,4,9],[2,5,8])
    scores = []
    sizes = []
    for l in xrange(len(groups)):
    #for l in ([0,3,7], [0,3,9], [0,7,9], [3,7,9], [0,2,4,5,8]):
       print groups[l]
       X_train,y_train,size = prepare_partial_prediction_data(train, groups[l])
       sizes.append(size)
       scores.append(find_best_prediction_classifier(X_train,y_train,classifiers,10))

    total = sum(sizes)*1.0
    print total
    overall = 0
    for i in xrange(len(scores)):
	    print scores[i], sizes[i], scores[i][1]*(sizes[i]/total)
	    overall += scores[i][1]*(sizes[i]/total)
    print overall


    # Load the prepared test set
    validation = load_from_file("validation")
    data,label= prepare_prediction_data(validation)
    X_train,y_train = prepare_joint_prediction_data(train)
    validation = load_from_file("validation")
    X_validation,y_validation = prepare_joint_prediction_data(validation)

    seperating_classifier = classifiers[separator[0]]
    #print "The best prediction model: ", chosen_classifier_name + "\n"

    seperating_classifier.fit(X_train, y_train)
    print seperating_classifier.score(X_validation, y_validation)


    print X_train
    print data
    prediction = seperating_classifier.predict(data)

    df = pd.DataFrame(data=prediction, columns=['Predicted-Vote'])
    # parties = ['Blues','Browns','Greens','Greys','Oranges','Pinks','Purples','Reds','Whites','Yellows']
    #
    # print "Parties prediction values:"
    # percentages = df.ix[:,0].value_counts(normalize=True,sort=False)
    # print percentages
    #for p in xrange(len(parties)):
    #    print parties[p],percentages[p]
    #
    # print "The Winning party is: " + parties[int(df.ix[:,0].value_counts(normalize=True).idxmax())] + "\n"
    validation = load_from_file("validation")
    predict = []
    validation['XVote'] = df
    X_validation,y_validation = prepare_prediction_data(validation)
    for i in xrange(len(scores)):
        X_train,y_train,size = prepare_partial_prediction_data(train, groups[i])
        orig_data = validation[validation['XVote'] == (i+1)]
        data,label = prepare_prediction_data(orig_data)
        print len(data)
        clf = classifiers[scores[i][0]].fit(X_train,y_train)
        score = classifiers[scores[i][0]].score(data,label)
        print score
        prd = clf.predict(data)
        #predict.append(prd)
        df = pd.DataFrame(data=prd, columns=['Predicted-Vote'])
        orig_data['Predicted-Vote'] = df

        predict.append(orig_data)
    for prd in predict:
        print prd



    return
    # df.to_csv("ElectionsData-predictvalueonly.csv",sep=',',index=False)
    # test.to_csv("ElectionsData-predict.csv",sep=',', index=False)

    # b. Construct the (test) confusion matrix and overall test error
    print "Confusion Matrix:"
    print confusion_matrix(y_test, prediction)
    print
    score = chosen_classifier.score(X_test, y_test)
    print chosen_classifier_name + " Score: %3f" % score
    print



main()
