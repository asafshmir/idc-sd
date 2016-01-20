__author__ = 'Jonatan & Baruch'

import numpy as np
import pandas as pd
from sklearn.cross_validation import train_test_split
from sklearn.linear_model import LinearRegression
from sklearn.preprocessing import Imputer, StandardScaler


RIGHT_FEATURE_SET = ['Vote', 'Yearly_ExpensesK', 'Yearly_IncomeK', 'Overall_happiness_score',
                     'Most_Important_Issue', 'Avg_Residancy_Altitude', 
                     'Will_vote_only_large_party', 'Financial_agenda_matters']

RIGHT_FEATURE_SET_PRED = ['IdentityCard_Num', 'Yearly_ExpensesK', 'Yearly_IncomeK', 'Overall_happiness_score',
                     'Most_Important_Issue', 'Avg_Residancy_Altitude',
                     'Will_vote_only_large_party', 'Financial_agenda_matters']


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


def save_to_file(filename,data,columns,name):
    df = pd.DataFrame(data=data,columns=columns)
    df.to_csv(filename + "-%s.csv" % name, sep=',',index=False)

def main():
	prepare_data("ElectionsData",[0.5,0.25,0.25], RIGHT_FEATURE_SET,True)
	prepare_data("ElectionsData_Pred_Features",[1,0,0],RIGHT_FEATURE_SET_PRED,False)

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

main()
