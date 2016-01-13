__author__ = 'Jonatan & Baruch'

import numpy as np
import pandas as pd
from sklearn.cluster import KMeans
from sklearn.mixture import GMM
from sklearn.naive_bayes import GaussianNB
from sklearn.cross_validation import train_test_split
from sklearn import cross_validation

import matplotlib.pyplot as plt
from mpl_toolkits.mplot3d import Axes3D

VOTES = {0:'Blues', 1:'Browns', 2:'Greens', 3:'Greys', 4:'Oranges', 5:'Pinks', 6:'Purples', 7:'Reds', 8:'Whites', 9:'Yellows'}
PARTIES = map(lambda x: x[1], sorted(VOTES.items()))
PARTS = 10

def load_from_file(name):
	return pd.read_csv("ElectionsData-%s.csv" % name, header=0)

def prepare_prediction_data(electionsData):
	x = electionsData.drop(['Vote'], axis=1).values
	y = electionsData.Vote.values
	return x,y

# Run cross validation prediction with different classifiers
def run_prediction_with_cross_validation(x,y,classifiers,parts):

	results = {}
	test = load_from_file("test")
	X_test,y_test = prepare_prediction_data(test)
	for classifier_name, classifier in classifiers.items():
		if (classifier_name == 'GMM'):
			scorer = lambda est, data: np.mean(est.score(data))
			scores = cross_validation.cross_val_score(classifier, x, cv=parts, scoring=scorer)
		else:
			scores = cross_validation.cross_val_score(classifier, x, y, cv=parts)
		results[np.mean(scores)] = classifier_name

		classifier.fit(x, y)
		scores = classifier.score(X_test, y_test)
		print classifier_name + ":", np.mean(scores)

	return results


def add_bin_plot(n_clusters,column, y_train, plot_spot,title):
	clf = KMeans(n_clusters=n_clusters)
	y_pred = clf.fit(column)
	values = y_pred.cluster_centers_.squeeze()
	labels = y_pred.labels_
	comp = np.choose(labels,values)

	plt.subplot(plot_spot)
	plt.scatter(comp, y_train, c=y_train)
	plt.title(title)


def plot_bins(X_train, y_train, X_test, y_test):

	n_clusters = 10

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


def plot_3d(train,relevant_parties=PARTIES):
	fig = plt.figure(figsize=(8,8))
	ax = fig.add_subplot(111, projection='3d')

	for i, name in zip(range(len(PARTIES)), PARTIES):
		if name in relevant_parties:
			cur = train[train.Vote==i]
			ax.scatter(cur.Overall_happiness_score, cur.Yearly_ExpensesK, cur.Yearly_IncomeK, label=name, color=name[:-1])

	ax.view_init(elev=65, azim=-71)

	plt.legend(loc='best')
	plt.show()


def find_coalition(train,clf):

	X_train = train.drop(['Vote','Financial_agenda_matters','Will_vote_only_large_party','Most_Important_Issue', 'Avg_Residancy_Altitude'], axis=1).values
	y_train = train.Vote.values

	clf.fit(X_train)
	clusters = clf.predict(X_train)

	print pd.crosstab(np.array(PARTIES)[y_train.astype(int)], clusters, rownames=["Party"], colnames=["Cluster"])

def main():

	# Load the prepared training set
	train = load_from_file("train")

	train_columns = train.drop(['Vote'], axis=1).columns
	X_train,y_train = prepare_prediction_data(train)

	# Load the prepared test set
	test = load_from_file("test")
	X_test,y_test = prepare_prediction_data(test)

	# Plotting all features as "bins"
	# Learnt that parties 1 and 6 are separate form the others according to
	# Avg Residancy Alt and Will Only Vote Large Party
	# Also Learnt that parties 2,5,8 are separate from the others using Most_Important_Issue
	# Also learnt that parties 2,5,6 are separate from the others using Financial_Agenda_Matters
	plot_bins(X_train, y_train, X_test, y_test)

	# Train classifiers (including cross validation) and check to see performance is ok
	# before we use them on our data
	classifiers = {
		"GMM" : GMM(n_components=5),
		"GaussianNB" : GaussianNB()
	}

	# Run with cross validation
	print "Test Scores:"
	res = run_prediction_with_cross_validation(X_train,y_train,classifiers,PARTS)
	keys = res.keys()
	keys.sort()
	keys.reverse()
	print "Cross Validation scores:"
	for key in keys:
		print "%s (%.3f)" % (res[key], key)


	# We Use a generative model. We look at non-categorical features
	# We learn that:
	# Yearly_Expenses are important to Blues, Greys, Reds, Yellows (below 0.5)
	# Yearly_Income are important to Blues, Greys, Reds, Yellows (below 0.5)
	# Overall_happinnes_score are important to Blues, Greys, Oranges, Reds, Yellows (below 0.5)
	# Avg_Alt_residenacy are important to no one, based on the (below 0.5) rule

	clf = GaussianNB()
	clf.fit(X_train, y_train)
	sigmas = pd.DataFrame(index=PARTIES, columns=train_columns, data=clf.sigma_)
	continous_features = ["Yearly_ExpensesK","Yearly_IncomeK","Overall_happiness_score","Avg_Residancy_Altitude"]
	print sigmas[continous_features]

	# Find coalition using clustering algorithm - GMM.
	# We see that the parties found in the generative algorithm above
	# are clustered together when run run GMM to find the coalition.
	# The relevant parties we find are indicated below.
	find_coalition(train,GMM(n_components=5))
	relevant_parties = ["Blues", "Yellows", "Reds", "Greys"]
	plot_3d(train,relevant_parties)

	# We saw that Oranges are very close to the other members of the coalition,
	# So we can strengthen the coalition by adding them.
	relevant_parties = ["Blues", "Yellows", "Reds", "Greys", "Oranges"]
	plot_3d(train,relevant_parties)

	# We chose to use GMM, but could easily use other clustering algorithms
	# We tried a few additional alternatives when testing, coming with rather similar results
	# but left these commented out
	#-find_coalition(train,KMeans(n_clusters=5,n_init=10),relevant_parties)
	#-relevant_parties = ["Blues", "Yellows", "Reds", "Greys", "Oranges"]
	#-plot_3d(train,relevant_parties)


	# The features we would change to form a different coalition are
	# Will_vote_only_large_party and Financial_agenda_matters, both set to -1
	# We changed our discriminative modeling script to check the results
	# and added that to the word document we submitted. It shows staggering
	# results with the brown party winning the elections and forming a coalition by itself

main()
