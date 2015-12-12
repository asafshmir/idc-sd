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
from sklearn.linear_model import Perceptron, LinearRegression
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

    "Decision Tree(10)" : DecisionTreeClassifier(max_depth=10),
    "Decision Tree(12)" : DecisionTreeClassifier(max_depth=12),
    "Decision Tree(13)" : DecisionTreeClassifier(max_depth=13),
    "Decision Tree(11)" : DecisionTreeClassifier(max_depth=11),
    
    "Nearest Neighbors(4)" : KNeighborsClassifier(4),
    "Nearest Neighbors(8)" : KNeighborsClassifier(8),
    "Nearest Neighbors(7)" : KNeighborsClassifier(7),
    
    "Nearest Neighbors(8)-distance" : KNeighborsClassifier(8, weights='distance'),  
    "Nearest Neighbors(9)-distance" : KNeighborsClassifier(9, weights='distance'),    
    "Nearest Neighbors(7)-distance" : KNeighborsClassifier(7, weights='distance'),      
    "Nearest Neighbors(6)-distance" : KNeighborsClassifier(6, weights='distance'), 
    "Nearest Neighbors(5)-distance" : KNeighborsClassifier(5, weights='distance'),    
    "Nearest Neighbors(4)-distance" : KNeighborsClassifier(4, weights='distance'),    

    "Naive Bayes" : GaussianNB(),
    
    "Perceptron" : Perceptron(n_iter=50),
    
    "Linear SVM OVO" : SVC(kernel="linear", C=1),
    
    "Linear SVM OVR" : LinearSVC(C=1)
    }   
        
    res = run_prediction_with_cross_validation(X_train,y_train,classifiers,5) 
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
    print "The best prediction model: ", chosen_classifier_name

    chosen_classifier.fit(X_train, y_train)
   
    # Use the selected model to provide the following:
    # a. Predict to which party each person in the test set will vote
    prediction = chosen_classifier.predict(X_test)

    # b. Construct the (test) confusion matrix and overall test error            
    print confusion_matrix(y_test, prediction)   
    score = chosen_classifier.score(X_test, y_test)
    print "Score: %3f" % score  

main()
