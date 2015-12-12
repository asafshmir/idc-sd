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

DEFAULT_TEST_SIZE = .3

def load_from_file(name):
    df = pd.read_csv("ElectionsData-%s.csv" % name, header=0)
    return df

# Prediction

def prepare_prediction_data(electionsData):
    x = electionsData.drop(['Vote'], axis=1).values
    y = electionsData.Vote.values
    return x,y

# Run prediction with different classifiers
def run_prediction(x,y,classifiers):

    X_train, X_test, y_train, y_test = train_test_split(x, y, test_size=DEFAULT_TEST_SIZE)
    
    for classifier_name, classifier in classifiers.items():
        
        classifier.fit(X_train, y_train)
        score = classifier.score(X_test, y_test)
        
        print  classifier_name, "Score: %3f" % score

# Run cross validation prediction with different classifiers
def run_prediction_with_cross_validation(x,y,classifiers):
    
    for classifier_name, classifier in classifiers.items():
        
        scores = cross_validation.cross_val_score(classifier, x, y, cv=5)
              
        print  classifier_name, scores


def main():
    
    # Load the prepared training set
    train = load_from_file("train")
    
    # Train at least two discriminative models (including cross validation)    
    x,y = prepare_prediction_data(train)
    
    classifiers = {
    "Decision Tree" : DecisionTreeClassifier(max_depth=15),
    "Nearest Neighbors" : KNeighborsClassifier(8),
    "Naive Bayes" : GaussianNB(),
    "Perceptron" : Perceptron(n_iter=50),
    "Linear SVM OVO" : SVC(kernel="linear", C=1),
    "Linear SVM OVR" : LinearSVC(C=1)
    }   
    
    run_prediction(x,y,classifiers)    
    
    run_prediction_with_cross_validation(x,y,classifiers) 
    
    # Load the prepared test set    
    test = load_from_file("test")
    
    # Apply the trained models on the test set and check performance
    
    # Select the best model for the prediction tasks
    
    # Use the selected model to provide the following:
    # a. Predict to which party each person in the test set will vote
    # b. Construct the (test) confusion matrix and overall test error

    sys.exit(1) ##############################################################

    x,y = prepare_prediction_data(electionsData,[])
    run_random_forest(x,y,electionsData.drop(['Vote'], axis=1).columns)

    classifiers = {
    "Decision Tree" : DecisionTreeClassifier(max_depth=15),
    "Nearest Neighbors" : KNeighborsClassifier(15)
    #"Naive Bayes" : GaussianNB(),
    #"Perceptron" : Perceptron(n_iter=50),
    #"Linear SVM OVO" : SVC(kernel="linear", C=1),
    #"Linear SVM OVR" : LinearSVC(C=1)
    }


    # We first ran on all features, and then iteratively selected which features we would like to add
    # We only left the run for our selected features, rest of the code was removed
    #run_classifiers(x,y,classifiers,tuple([]),electionsData.columns,3)

    strong_features = ['Yearly_IncomeK',
                       'Overall_happiness_score',
                       'Yearly_ExpensesK',
                       'Last_school_grades',
                       'Looking_at_poles_results',
                       'Financial_agenda_matters',
                       'Political_interest_Total_Score',
                       'Married',
                       'Most_Important_Issue',
                       'Avg_monthly_expense_when_under_age_21']


    fixed_features = tuple(map(lambda c : list(electionsData.columns).index(c)-1, strong_features))
    run_classifiers(x,y,classifiers,fixed_features,electionsData.columns,0)

    selected_features = [list(electionsData.columns)[i+1] for i in range(len(electionsData.columns)-1) if i in fixed_features]
    drop_features = [list(electionsData.columns)[i+1] for i in range(len(electionsData.columns)-1) if i not in fixed_features]

    x,y = prepare_prediction_data(electionsData,drop_features)
    run_random_forest(x,y,selected_features)


    originalFeatures = originalElectionsData.columns
    for feature in originalFeatures:
        if not(feature in (selected_features + ['Vote'])):
            originalElectionsData = originalElectionsData.drop([feature],axis=1)

    train, test, validation = split_data(originalElectionsData)
    save_to_file(train,originalElectionsData.columns,"trainOrig")
    save_to_file(test,originalElectionsData.columns,"testOrig")
    save_to_file(validation,originalElectionsData.columns,"validationOrig")

    electionsData = electionsData.drop(drop_features,axis=1)
    train, test, validation = split_data(electionsData)
    save_to_file(train,electionsData.columns,"train")
    save_to_file(test,electionsData.columns,"test")
    save_to_file(validation,electionsData.columns,"validation")

main()




############################################### OLD SCRIPT ##################################


def run_random_forest(x,y,features):
    X_train, X_test, y_train, y_test = train_test_split(x, y, test_size=DEFAULT_TEST_SIZE)
    clf = RandomForestClassifier(n_estimators=40, max_features=7, n_jobs=-1)
    clf.fit(X_train,y_train)
    score = clf.score(X_test, y_test)

    scored_features = []
    for i in xrange(len(clf.feature_importances_)):
        scored_features.append(tuple([clf.feature_importances_[i],features[i]]))
    scored_features = sorted(scored_features,reverse=True)
    print "Random Forest Score: " + str(score)

    # Print top 10 features
    for feature in scored_features[:10]:
        print "Score: %3f" % feature[0], "\tFeature: %s" % feature[1]
    print "\n"


# Run classifiers, trying to add 2,3 features each time.
def run_classifiers(x,y,classifiers,fixed_features,columns,features_to_add):

    X_train, X_test, y_train, y_test = train_test_split(x, y, test_size=DEFAULT_TEST_SIZE)
    features_pool = [i for i in range(X_train.shape[1]) if i not in fixed_features]

    for new_features_amount in xrange(0,1+features_to_add):
        print "Feature Amount: " + str(new_features_amount)
        for classifier_name, classifier in classifiers.items():
            max_score = 0
            max_features = (0,)*(len(fixed_features) + new_features_amount)

            for features in combinations(features_pool, new_features_amount):
                X_train_selected = X_train[:,fixed_features+features]
                X_test_selected = X_test[:,fixed_features+features]

                classifier.fit(X_train_selected, y_train)
                score = classifier.score(X_test_selected, y_test)
                if (score > max_score):
                    max_score = round(score,3)
                    max_features = tuple(features)

            print  classifier_name, "Score: %3f" % max_score
            print "Selected Features:"
            selected_features = fixed_features+max_features
            for i in xrange(len(selected_features)):
                print columns[selected_features[i]+1],
            print "\n"
