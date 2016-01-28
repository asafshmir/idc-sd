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

RIGHT_FEATURE_SET = ['Vote', 'Yearly_ExpensesK', 'Yearly_IncomeK', 'Overall_happiness_score',
                     'Most_Important_Issue', 'Avg_Residancy_Altitude',
                     'Will_vote_only_large_party', 'Financial_agenda_matters']

RIGHT_FEATURE_SET_PRED = ['IdentityCard_Num', 'Yearly_ExpensesK', 'Yearly_IncomeK', 'Overall_happiness_score',
                     'Most_Important_Issue', 'Avg_Residancy_Altitude',
                     'Will_vote_only_large_party', 'Financial_agenda_matters']

VOTES = {0:'Blues', 1:'Browns', 2:'Greens', 3:'Greys', 4:'Oranges', 5:'Pinks', 6:'Purples', 7:'Reds', 8:'Whites', 9:'Yellows'}
PARTIES = map(lambda x: x[1], sorted(VOTES.items()))
CV_PARTS = 10
GROUPS = ([1,6], [2,5,8],[0,4,9,3,7])
CLASSIFIERS = [{
    "Random Forest (11)" : RandomForestClassifier(max_depth=11, n_estimators=25, max_features=5),
    "BaggingClassifier(ExtraTrees11)" : BaggingClassifier(RandomForestClassifier(max_depth=11, n_estimators=25, max_features=5)),
    },
     {
     "Random Forest (11)" : RandomForestClassifier(max_depth=11, n_estimators=25, max_features=5),
     "ExtraTreesClassifier (11)" : ExtraTreesClassifier(max_depth=11, n_estimators=25, max_features=5),
     "BaggingClassifier(ExtraTrees11)" : BaggingClassifier(RandomForestClassifier(max_depth=11, n_estimators=25, max_features=5)),
    "Linear SVM linear" : SVC(kernel="linear", C=100),
    "Linear SVM rbf" : SVC(kernel="rbf", C=100),
    # "AdaBoost1" : AdaBoostClassifier(base_estimator=ExtraTreesClassifier(max_depth=11, n_estimators=25, max_features=8)),
    #  "AdaBoost2" : AdaBoostClassifier(base_estimator=SVC(kernel="rbf", C=100), algorithm = 'SAMME')
    },
     {
      "Random Forest (11)" : RandomForestClassifier(max_depth=11, n_estimators=25, max_features=5),
     "ExtraTreesClassifier (11)" : ExtraTreesClassifier(max_depth=12, n_estimators=25, max_features=5),
     "BaggingClassifier(ExtraTrees11)" : BaggingClassifier(ExtraTreesClassifier(max_depth=11, n_estimators=25, max_features=3)),
      "Linear SVM linear" : SVC(kernel="linear", C=100),
     "Linear SVM rbf" : SVC(kernel="rbf", C=100),
    # "AdaBoost1" : AdaBoostClassifier(base_estimator=ExtraTreesClassifier(max_depth=11, n_estimators=25, max_features=8)),
    #  "AdaBoost2" : AdaBoostClassifier(base_estimator=SVC(kernel="rbf", C=100), algorithm = 'SAMME')
    },
     {
     "Random Forest (11)" : RandomForestClassifier(max_depth=11, n_estimators=25, max_features=5),
      "ExtraTreesClassifier (11)" : ExtraTreesClassifier(max_depth=11, n_estimators=25, max_features=5),
     "BaggingClassifier(ExtraTrees11)" : BaggingClassifier(RandomForestClassifier(max_depth=11, n_estimators=25, max_features=5)),
     "Linear SVM linear" : SVC(kernel="linear", C=100),
     "Linear SVM rbf" : SVC(kernel="rbf", C=100),
      "SVM poly" : SVC(kernel='poly',C=100),
     # "AdaBoost1" : AdaBoostClassifier(base_estimator=ExtraTreesClassifier(max_depth=11, n_estimators=25, max_features=8)),
     # "AdaBoost2" : AdaBoostClassifier(base_estimator=SVC(kernel="rbf", C=100), algorithm = 'SAMME')
    }]
DEFAULT_TEST_SIZE = .3



# Format conversion - Transfer categorical features to boolean and numerical ones.
def convert_categorial_features(electionsData):
    # Convert Yes/No Categories to boolean type
    binary_features = ['Looking_at_poles_results', 'Married', 'Will_vote_only_large_party', 'Financial_agenda_matters']
    for f in binary_features:
        electionsData[f] = electionsData[f].map( {'No': -1, 'Yes': 1}).astype("float64")

    electionsData['Gender'] = electionsData['Gender'].map( {'Male': -1, 'Female': 1})
    electionsData['Voting_Time'] = electionsData['Voting_Time'].map({'By_16:00': -1, 'After_16:00': 1})
    electionsData['Age_group'] = electionsData['Age_group'].map({'Below_30': 18, '30-45':38, '45_and_up':55}).astype("float64")

    # Identify which of the orginal features are objects
    ObjFeat = electionsData.keys()[electionsData.dtypes.map(lambda x: x=='object')]

    # Transform the original features to categorical
    ed_copy = electionsData.copy()
    for f in ObjFeat:
        electionsData[f] = electionsData[f].astype("category")
        electionsData[f] = electionsData[f].cat.rename_categories(range(electionsData[f].nunique())).astype(int)
        electionsData.loc[ed_copy[f].isnull(), f] = np.nan #fix NaN conversion

    return [electionsData, ObjFeat.tolist() + binary_features + ['Age_group', 'Gender', 'Voting_Time', 'Number_of_differnt_parties_voted_for', 'Number_of_valued_Kneset_members','Num_of_kids_born_last_10_years']]


# Drop Outlier negative values found
def drop_outliers(electionsData):
    electionsData = electionsData.drop(electionsData[electionsData.Avg_monthly_expense_when_under_age_21 < 0].index)
    electionsData = electionsData.drop(electionsData[electionsData.AVG_lottary_expanses < 0].index)
    electionsData = electionsData.drop(electionsData[electionsData.Avg_Residancy_Altitude < 0].index)
    return electionsData

# Find correlated features
def find_correlations(electionsData):

    # find most correlated features
    correlations = []
    num_largest = 16
    full = electionsData.corr().as_matrix()
    for i in xrange(len(full)):
        for j in xrange(len(full)):
            if (i == j):
                full[i][j] = 0
            else:
                full[i][j] = abs(full[i][j])
    indices = (-full).argpartition(num_largest, axis=None)[:num_largest]

    x, y = np.unravel_index(indices, full.shape)

    for i in xrange(len(x)):
        correlations .append((electionsData.keys()[x[i]],electionsData.keys()[y[i]]))

    return correlations

#Use the correlated columns to fill missing values
def fill_missing_linear_regression(electionsData, correlations):

    linearRegression = LinearRegression()
    for x,y in correlations:
        correlated_features = electionsData[[x,y]].dropna()
        to_predict = electionsData[electionsData[x].isnull()][y].dropna()
        linearRegression.fit(correlated_features[y].values.reshape(-1,1), correlated_features[x].values.reshape(-1,1))
        for v in to_predict.index:
            filler_value = linearRegression.predict(to_predict[v])[0][0]
            electionsData[x][v] = filler_value


# Imputation & Scaling

def split_categorial(electionsData,categorical_features):

    most_frequent = categorical_features
    non_categorical = electionsData.keys().tolist()

    for m in most_frequent:
        if m in non_categorical:
            non_categorical.remove(m)

    return most_frequent, non_categorical

# Fill in other missing values
def fill_missing_imputation(electionsData, most_frequent):

    most_frequent = electionsData.columns.intersection(most_frequent)

    im = Imputer(strategy="most_frequent")
    electionsData[most_frequent] = im.fit_transform(electionsData[most_frequent])

    #Fill all of the rest (numeric) using mean
    im = Imputer(strategy="median")
    electionsData[:] = im.fit_transform(electionsData[:])


#Investigate the remaining interesting columns:
def scale(electionsData):

    # Standard scaling
    normally_disributed_features = ['Avg_Residancy_Altitude',
                                    'Yearly_ExpensesK',
                                    'Yearly_IncomeK',
                                    'Overall_happiness_score']

    col_scale = electionsData.columns.intersection(normally_disributed_features)

    s = StandardScaler()
    electionsData[col_scale] = pd.DataFrame(s.fit_transform(electionsData[col_scale]), index=electionsData[col_scale].index)
    return electionsData

def split_data(split_size,data):
	data_size = len(data)

	indexes = np.random.permutation(data_size)

	data_sets = []
	for i in xrange(len(split_size)):
		index1 = int(sum(split_size[0:i])*data_size)
		index2 = int(sum(split_size[0:i+1])*data_size)
		indexes_new = [data.index[i] for i in indexes[index1:index2]]
		data_sets.append(data.loc[indexes_new,:])

	return data_sets



def prepare_data(filename, split_sizes,right_feature_set,split):
    # Read and "backup" the elections data
    electionsData = pd.read_csv(filename + ".csv", header=0)
    originalElectionsData = electionsData.copy()

    electionsData , categorical_features = convert_categorial_features(electionsData)

    if split:
        electionsData = drop_outliers(electionsData)

    correlations = find_correlations(electionsData)

    fill_missing_linear_regression(electionsData,correlations)

    # Drop unnecessary features (from both election-data and the backup)
    electionsData = electionsData[right_feature_set]
    originalElectionsData  = originalElectionsData[right_feature_set]

    most_frequent, non_categorical = split_categorial(electionsData,categorical_features)

    fill_missing_imputation(electionsData,most_frequent)

    electionsData = scale(electionsData)

    if split:
        train, test, validation = split_data(split_sizes ,originalElectionsData)
        save_to_file(filename,train,originalElectionsData.columns,"trainOrig")
        save_to_file(filename,test,originalElectionsData.columns,"testOrig")
        save_to_file(filename,validation,originalElectionsData.columns,"validationOrig")

        train, test, validation = split_data(split_sizes, electionsData)
        save_to_file(filename,train,electionsData.columns,"train")
        save_to_file(filename,test,electionsData.columns,"test")
        save_to_file(filename,validation,electionsData.columns,"validation")
    else:
        save_to_file(filename,electionsData,electionsData.columns,"pred")

def save_to_file(filename,data,columns,name):
    df = pd.DataFrame(data=data,columns=columns)
    df.to_csv(filename + "-%s.csv" % name, sep=',',index=False)

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
        #x = electionsData[['Overall_happiness_score','Yearly_ExpensesK', 'Yearly_IncomeK']].values

        x = electionsData[['Most_Important_Issue', 'Yearly_ExpensesK', 'Yearly_IncomeK', 'Overall_happiness_score']].values
        #x = electionsData[['Yearly_IncomeK','Yearly_ExpensesK','Most_Important_Issue','Will_vote_only_large_party','Financial_agenda_matters','Avg_Residancy_Altitude','Overall_happiness_score']].values
        #'Most_Important_Issue','Will_vote_only_large_party','Financial_agenda_matters','Avg_Residancy_Altitude','Overall_happiness_score'
    else:
        x = electionsData.drop(['Vote'], axis=1).values
    y = electionsData.Vote.values
    return x,y, len(electionsData.index)

# Run cross validation prediction with different classifiers
def run_prediction_with_cross_validation(x,y,x_svc,y_svc,classifiers,parts):

    results = {}

    for classifier_name, classifier in classifiers.items():
        if (isinstance(classifier,SVC) and x_svc != []):
            scores = cross_validation.cross_val_score(classifier, x_svc, y_svc, cv=parts)
        else:
            scores = cross_validation.cross_val_score(classifier, x, y, cv=parts)
        results[np.mean(scores)] = classifier_name

    return results

def find_best_prediction_classifier(x,y,classifiers,parts,x_svc=[],y_svc=[]):
    res = run_prediction_with_cross_validation(x,y,x_svc,y_svc,classifiers,parts)

    keys = res.keys()
    keys.sort()
    keys.reverse()
    print "Model scores:"
    for key in keys:
        print "%s (%.3f)" % (res[key], key)
    return res[keys[0]],keys[0]

def find_classifiers():

    train = load_from_file("train")

    print "Seperating groups:"
    X_train,y_train = prepare_joint_prediction_data(train)
    classifier_name,score = find_best_prediction_classifier(X_train,y_train,CLASSIFIERS[0],CV_PARTS)
    print X_train.shape, classifier_name
    train = load_from_file("train")
    X_train,y_train = prepare_joint_prediction_data(train)

    seperating_classifier = copy.copy(CLASSIFIERS[0][classifier_name]).fit(X_train, y_train)

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
       classifier_name, classifier_score = find_best_prediction_classifier(X_train,y_train, CLASSIFIERS[l],CV_PARTS, X_SVC_train,y_SVC_train)

       best_classifiers.append(copy.copy(CLASSIFIERS[l][classifier_name]).fit(X_train,y_train))
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

    validation = pd.read_csv("ElectionsData_Pred_Features-pred.csv", header=0)

    data = validation.drop(['IdentityCard_Num'],axis=1).values

    validation['XVOTE'] = seperating_classifier.predict(data)

    predict = []

    for i in xrange(len(group_classifiers)):
        validation_sub_group = validation[validation['XVOTE'] == (i+1)]
        validation_sub_group ['PredictVote'] = group_classifiers[i].predict(validation_sub_group.drop(['IdentityCard_Num'],axis=1).values)
        predict.append(validation_sub_group)

    test = pd.concat(predict,axis=0)

    return test


def validate_classification(seperating_classifier,group_classifiers, VALIDATION_FILE="validation"):

    # Load the prepared test set

    validation = load_from_file(VALIDATION_FILE)
    data,label= prepare_prediction_data(validation)
    prediction = seperating_classifier.predict(data)

    df = pd.DataFrame(data=prediction, columns=['XVOTE'])

    validation = load_from_file(VALIDATION_FILE)
    predict = []
    validation.loc[:,('XVOTE')] = df

    for i in xrange(len(group_classifiers)):
        #X_train,y_train,size = prepare_partial_prediction_data(train, GROUPS[i])
        validation_sub_group = validation[validation['XVOTE'] == (i+1)]

        data,label = prepare_prediction_data(validation_sub_group )

        clf = group_classifiers[i]
        score = clf.score(data,label)

        print GROUPS[i],score
        validation_sub_group.loc[:,('PredictVote')] = clf.predict(data)

        predict.append(validation_sub_group)

    test = pd.concat(predict,axis=0)

    print "Validation Score: " + str(len(test[test['Vote'] == test['PredictVote']])/(len(test)*1.0))
    return test

def summarize_prediction(test):

    print VOTES[test['PredictVote'].value_counts().idxmax()]

    percentages = test['PredictVote'].value_counts(normalize=True,sort=False)
    print percentages
    for p in xrange(len(PARTIES)):
        print PARTIES[p],percentages[p]



def plot_3d(train,relevant_parties=PARTIES):
	fig = plt.figure(figsize=(8,8))
	ax = fig.add_subplot(111, projection='3d')

	for i, name in zip(range(len(PARTIES)), PARTIES):
		if i in relevant_parties:

			cur = train[train['PredictVote']==i]
			ax.scatter(cur.Overall_happiness_score, cur.Yearly_ExpensesK, cur.Yearly_IncomeK, label=name, color=name[:-1])


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

    #prepare_data("ElectionsData",[0.5,0.25,0.25], RIGHT_FEATURE_SET,True)
    #prepare_data("ElectionsData_Pred_Features",[1,0,0],RIGHT_FEATURE_SET_PRED,False)

    seperating_classifier,group_classifiers = find_classifiers()

    #prediction = validate_classification(seperating_classifier,group_classifiers)
    validate_classification(seperating_classifier,group_classifiers)
    prediction = predict_elections(seperating_classifier,group_classifiers)

    summarize_prediction(prediction)

    # plot_3d(prediction,[0,7,9])
    # plot_3d(prediction,[0,4,7,9])
    # plot_3d(prediction,[0,3,7,9])
    # plot_3d(prediction,[0,3,4,7,9])
    # plot_bins(prediction.drop(['PredictVote'],axis=1).values, prediction['PredictVote'].values)

    out_columns = ['IdentityCard_Num','PredictVote']
    prediction.loc[:,('PredictVote')] = prediction['PredictVote'].map(VOTES)
    prediction.loc[:,('IdentityCard_Num')] = prediction['IdentityCard_Num'].astype(int)

    df = pd.DataFrame(data=prediction[out_columns],columns=out_columns)
    df = df.sort(['IdentityCard_Num'],ascending=[1])
    df.to_csv("ElectionsPrediction.csv", sep=',',index=False)








main()
