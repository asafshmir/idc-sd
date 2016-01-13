__author__ = 'Jonatan & Baruch'

import sys

import numpy as np
import pandas as pd
from sklearn.naive_bayes import GaussianNB
from sklearn.neighbors import KNeighborsClassifier
from sklearn.cross_validation import train_test_split
from sklearn import cross_validation
from sklearn.ensemble import RandomForestClassifier
from sklearn.tree import DecisionTreeClassifier
from sklearn.linear_model import Perceptron, LinearRegression, LogisticRegression

from sklearn.svm import SVC, LinearSVC
from sklearn.preprocessing import Imputer, StandardScaler

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
    "Decision Tree(10)-entropy" : DecisionTreeClassifier(max_depth=10, criterion="entropy"),
    "Decision Tree(11)-entropy" : DecisionTreeClassifier(max_depth=11, criterion="entropy"),
    "Decision Tree(12)-entropy" : DecisionTreeClassifier(max_depth=12, criterion="entropy"), 
    "Decision Tree(13)-entropy" : DecisionTreeClassifier(max_depth=13, criterion="entropy"),

    "Decision Tree(10)" : DecisionTreeClassifier(max_depth=10), # Gini is default
    "Decision Tree(12)" : DecisionTreeClassifier(max_depth=12), # Gini is default
    "Decision Tree(13)" : DecisionTreeClassifier(max_depth=13), # Gini is default
    "Decision Tree(11)" : DecisionTreeClassifier(max_depth=11), # Gini is default

	#"Random Forest (8)" : RandomForestClassifier(max_depth=8, n_estimators=20, max_features=5),
	"Random Forest (9)" : RandomForestClassifier(max_depth=9, n_estimators=25, max_features=5),
    "Random Forest (10)" : RandomForestClassifier(max_depth=10, n_estimators=25, max_features=5),
    "Random Forest (11)" : RandomForestClassifier(max_depth=11, n_estimators=25, max_features=5),
    #"Random Forest (12)" : RandomForestClassifier(max_depth=12, n_estimators=20, max_features=5),

     #"LinearRegression" :  LinearRegression(),
     #"LogisticRegression" :  LogisticRegression(),
     #
     # "Nearest Neighbors(4)" : KNeighborsClassifier(4),
     # "Nearest Neighbors(8)" : KNeighborsClassifier(8),
     # "Nearest Neighbors(7)" : KNeighborsClassifier(7),
     #
     # "Nearest Neighbors(8)-distance" : KNeighborsClassifier(8, weights='distance'),
     # "Nearest Neighbors(9)-distance" : KNeighborsClassifier(9, weights='distance'),
     # "Nearest Neighbors(7)-distance" : KNeighborsClassifier(7, weights='distance'),
     # "Nearest Neighbors(6)-distance" : KNeighborsClassifier(6, weights='distance'),
     # "Nearest Neighbors(5)-distance" : KNeighborsClassifier(5, weights='distance'),
     # "Nearest Neighbors(4)-distance" : KNeighborsClassifier(4, weights='distance'),
     #
     # "Naive Bayes" : GaussianNB(),
     #
     # "Perceptron" : Perceptron(n_iter=50),
     #
     # "Linear SVM OVO" : SVC(kernel="linear", C=1),
     #
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
    test.loc[:,'Will_vote_only_large_party'] = -1
    #test.loc[:,'Avg_Residancy_Altitude'] = 1
    #test.loc[:,'Yearly_ExpensesK'] = 0
    test.loc[:,'Financial_agenda_matters'] = -1
    #test.loc[:,'Most_Important_Issue'] = 0
    #test.loc[:,'Yearly_IncomeK'] = 0
    #test.loc[:,'Overall_happiness_score'] = 0

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
    print prediction

    df = pd.DataFrame(data=prediction, columns=['Predicted-Vote'])
    parties = ['Blues','Browns','Greens','Greys','Oranges','Pinks','Purples','Reds','Whites','Yellows']

    print "Parties prediction values:"
    percentages = df.ix[:,0].value_counts(normalize=True,sort=False)
    print percentages
    #for p in xrange(len(parties)):
    #    print parties[p],percentages[p]

    print "The Winning party is: " + parties[int(df.ix[:,0].value_counts(normalize=True).idxmax())] + "\n"

    test['Predicted-Vote'] = df
    df.to_csv("ElectionsData-predictvalueonly.csv",sep=',',index=False)
    test.to_csv("ElectionsData-predict.csv",sep=',', index=False)

    # b. Construct the (test) confusion matrix and overall test error
    print "Confusion Matrix:"
    print confusion_matrix(y_test, prediction)
    print
    score = chosen_classifier.score(X_test, y_test)
    print chosen_classifier_name + " Score: %3f" % score
    print



main()
