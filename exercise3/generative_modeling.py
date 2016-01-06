__author__ = 'Jonatan & Baruch'

import sys

import numpy as np
import pandas as pd
from sklearn.cluster import KMeans, AgglomerativeClustering
import sklearn.cluster as cluster

import matplotlib.pyplot as plt
from mpl_toolkits.mplot3d import Axes3D

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


from sklearn.metrics import confusion_matrix


import itertools

from scipy import linalg
import matplotlib as mpl

from sklearn import mixture


DEFAULT_TEST_SIZE = .3

RIGHT_FEATURE_SET = ['Vote', 'Yearly_ExpensesK', 'Yearly_IncomeK', 'Overall_happiness_score',
                     'Most_Important_Issue', 'Avg_Residancy_Altitude',
                     'Will_vote_only_large_party', 'Financial_agenda_matters']

VOTES = {0:'Blues', 1:'Browns', 2:'Greens', 3:'Greys', 4:'Oranges', 5:'Pinks', 6:'Purples', 7:'Reds', 8:'Whites', 9:'Yellows'}
PARTIES = map(lambda x: x[1], sorted(VOTES.items()))

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

def add_kmeans_plot(n_clusters,column, y_train,plot_spot,title):
	y_pred = KMeans(n_clusters=n_clusters).fit(column)
	print y_pred.cluster_centers_
	values = y_pred.cluster_centers_.squeeze()
	labels = y_pred.labels_
	comp = np.choose(labels,values)

	plt.subplot(plot_spot)
	plt.scatter(comp, y_train, c=y_train)
	plt.title(title)


def add_gmm_plot(n_clusters,column, y_train,plot_spot,title):
	y_pred = GMM(n_components=n_clusters).fit(column)
	print y_pred.cluster_centers_
	values = y_pred.cluster_centers_.squeeze()
	labels = y_pred.labels_
	comp = np.choose(labels,values)

	plt.subplot(plot_spot)
	plt.scatter(comp, y_train, c=y_train)
	plt.title(title)

def plot_gmm(X_train):

	# Number of samples per component
	n_samples = 500

	# Generate random sample, two components
	np.random.seed(0)
	C = np.array([[0., -0.1], [1.7, .4]])
	# X = np.r_[np.dot(np.random.randn(n_samples, 2), C),
	#           .7 * np.random.randn(n_samples, 2) + np.array([-6, 3])]
	X = X_train
	lowest_bic = np.infty
	bic = []
	n_components_range = range(1, 2)
	cv_types = ['spherical', 'tied', 'diag', 'full']
	for cv_type in cv_types:
		for n_components in n_components_range:
			# Fit a mixture of Gaussians with EM
			gmm = mixture.GMM(n_components=n_components, covariance_type=cv_type)
			gmm.fit(X)
			bic.append(gmm.bic(X))
			if bic[-1] < lowest_bic:
				lowest_bic = bic[-1]
				best_gmm = gmm

	bic = np.array(bic)
	color_iter = itertools.cycle(['k', 'r', 'g', 'b', 'c', 'm', 'y','w'])
	#color_iter = [(1,1,1),(1,1,64),(1,1,128),(1,1,192),(1,1,256)]
	clf = best_gmm
	bars = []

	# Plot the BIC scores
	spl = plt.subplot(2, 1, 2)
	for i, (cv_type, color) in enumerate(zip(cv_types, color_iter)):
	    xpos = np.array(n_components_range) + .2 * (i - 2)
	    bars.append(plt.bar(xpos, bic[i * len(n_components_range):(i + 1) * len(n_components_range)],
	                        width=.2, color=color))
	plt.xticks(n_components_range)
	plt.ylim([bic.min() * 1.01 - .01 * bic.max(), bic.max()])
	plt.title('BIC score per model')
	xpos = np.mod(bic.argmin(), len(n_components_range)) + .65 + \
	       .2 * np.floor(bic.argmin() / len(n_components_range))
	plt.text(xpos, bic.min() * 0.97 + .03 * bic.max(), '*', fontsize=14)
	spl.set_xlabel('Number of components')
	spl.legend([b[0] for b in bars], cv_types)

	# Plot the winner
	splot = plt.subplot(2, 1, 1)
	Y_ = clf.predict(X)
	for i, (mean, covar, color) in enumerate(zip(clf.means_, clf.covars_,
	                                             color_iter)):
	    v, w = linalg.eigh(covar)
	    if not np.any(Y_ == i):
	        continue
	    plt.scatter(X[Y_ == i, 0], X[Y_ == i, 2], .8, color=color)

	    # Plot an ellipse to show the Gaussian component
	    angle = np.arctan2(w[0][1], w[0][0])
	    angle = 180 * angle / np.pi  # convert to degrees
	    v *= 4
	    ell = mpl.patches.Ellipse(mean, v[0], v[1], 180 + angle, color=color)
	    ell.set_clip_box(splot.bbox)
	    ell.set_alpha(.5)
	    splot.add_artist(ell)

	plt.xlim(-10, 10)
	plt.ylim(-3, 6)
	plt.xticks(())
	plt.yticks(())
	plt.title('Selected GMM: full model, 2 components')
	plt.subplots_adjust(hspace=.35, bottom=.02)
	plt.show()


def plot_kmeans(X_train, y_train):

	#run_kmeans(X_train,y_train)



	n_clusters = 10
	add_kmeans_plot(n_clusters,X_train[:, 0:1], y_train,431,"Yearly_Expense")
	add_kmeans_plot(n_clusters,X_train[:, 1:2], y_train,432,"Yearly_Income")
	add_kmeans_plot(n_clusters,X_train[:, 2:3], y_train,433,"Overall_Happiness_Score")
	add_kmeans_plot(n_clusters,X_train[:, 5:6], y_train,435,"Avg_Residancy_Alt")

	# plt.subplot(435)
	# plt.scatter(X_train[:, 4], y_train, c=y_train)
	# plt.title()

	# plt.subplot(431)
	# plt.scatter(, y_train, c=y_train)
	# plt.title()
	#
	# plt.subplot(432)
	# plt.scatter(X_train[:, 1], y_train, c=y_train)
	# plt.title()

	# plt.subplot(433)
	# plt.scatter(X_train[:, 2], y_train, c=y_train)
	# plt.title()

	plt.subplot(434)
	plt.scatter(X_train[:, 3], y_train, c=y_train)
	plt.title("Most_Important_Issue")

	plt.subplot(436)
	plt.scatter(X_train[:, 5], y_train, c=y_train)
	plt.title("Will_only_vote_large_party")

	plt.subplot(437)
	plt.scatter(X_train[:, 6], y_train, c=y_train)
	plt.title("Financial_Agenda_matters")
	#
	# ax = fig.add_subplot(211,projection='3d')
	# tr = train[train['Vote'].isin([0,1,5])].values
	#
	# ax.scatter(tr[:, 1], tr[:,2],tr[:,3], c=tr[:,0], marker='o')
	# ax.set_xlabel('Sepal length')
	# ax.set_ylabel('Sepal width')
	#ax.title("Financial_Agenda_matters")

	# plt.subplot(211,projection='3d')
	# plt.scatter(X_train[:, 0], X_train[:,1],X_train[:,2], c=y_train, marker='o')
	# plt.xlabel('Sepal length')
	# plt.ylabel('Sepal width')
	# plt.title("Financial_Agenda_matters")


	plt.show()


def plot_3d(train,relevant_parties=PARTIES):
	fig3 = plt.figure(figsize=(8,8))
	ax3 = fig3.add_subplot(111, projection='3d')
	fig2 = plt.figure(figsize=(8,8))
	ax2 = fig2.add_subplot(111, projection='3d')
	fig1 = plt.figure(figsize=(8,8))
	ax1 = fig1.add_subplot(111, projection='3d')

	#relevant_parties = ["Blues", "Yellows", "Reds", "Greys", "Oranges"] #-155,-57
	#relevant_parties = ["Greens","Pinks","Whites"] # -45, -45

	for i, name in zip(range(len(PARTIES)), PARTIES):
		if name in relevant_parties:
			cur = train[train.Vote==i]
			ax3.scatter(cur.Yearly_ExpensesK, cur.Yearly_IncomeK, cur.Overall_happiness_score, label=name, color=name[:-1])
			ax2.scatter(cur.Yearly_ExpensesK, cur.Yearly_IncomeK, cur.Overall_happiness_score, label=name, color=name[:-1])
			ax1.scatter(cur.Overall_happiness_score, cur.Yearly_ExpensesK, cur.Yearly_IncomeK, label=name, color=name[:-1])

	ax1.view_init(elev=100, azim=320)
	ax2.view_init(elev=180, azim=310)
	ax3.view_init(elev=45, azim=45)

	plt.legend(loc='best')
	plt.show()

def find_coalition(train,clf):

	X_train = train.drop(['Vote','Financial_agenda_matters','Will_vote_only_large_party','Most_Important_Issue', 'Avg_Residancy_Altitude'], axis=1).values
	y_train = train.Vote.values

	clf.fit(X_train)
	clusters = clf.predict(X_train)

	print pd.crosstab(np.array(PARTIES)[y_train.astype(int)], clusters, rownames=["Party"], colnames=["Cluster"])
	relevant_parties = ["Blues", "Yellows", "Reds", "Greys", "Oranges"]
	coalition = ["Blues", "Yellows", "Oranges"]


	plot_3d(train,relevant_parties)


def main():

	# Load the prepared training set
	train = load_from_file("train")
	train_columns = train.drop(['Vote'], axis=1).columns
	X_train,y_train = prepare_prediction_data(train)

	# Load the prepared test set
	test = load_from_file("test")
	X_test,y_test = prepare_prediction_data(test)

	# Learnt that parties 1 and 6 are separate form the others according to
	# Avg Residancy Alt and Will Only Vote Large Party

	# Also Learnt that parties 2,5,8 are separate from the others using
	# Most_Important_Issue


	# TODO - uncomment
	#plot_kmeans(X_train, y_train)

	# We focus on the rest 0,3,4,7,9

	# Use a generative model. We look at non-categorical features
	# We learn that:
	# Yearly_Expenses are important to Blues, Greys, Reds, Yellows (below 0.5)
	# Yearly_Income are important to Blues, Greys, Reds, Yellows (below 0.5)
	# Overall_happinnes_score are important to Blues, Greys, Oranges, Reds, Yellows (below 0.5)
	# Avg_Alt_residenacy are important to no one, based on the (below 0.5) rule
	clf = GaussianNB()
	clf.fit(X_train, y_train)
	sigmas = pd.DataFrame(index=PARTIES, columns=train_columns, data=clf.sigma_)
	continous_features = ["Yearly_ExpensesK","Yearly_IncomeK","Overall_happiness_score","Avg_Residancy_Altitude"]
	print sigmas[continous_features ]

	# TODO - uncomment and show all groups
	#plot_3d(train)
	#plot_gmm(X_train)
	clf = GMM(n_components=5)
	find_coalition(train,clf)
	clf = KMeans(n_clusters=5)
	find_coalition(train,clf)

	return

	# Train at least two discriminative models (including cross validation)
	classifiers = {
		# Clustering Algorithms
		# "KMeans (2)" : KMeans(n_clusters=2, n_init=10),

		# "GMM" : GMM,

		# "GaussianNB" : GaussianNB,

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


	df = pd.DataFrame(data=X_train)
	parties = ['Blues','Browns','Greens','Greys','Oranges','Pinks','Purples','Reds','Whites','Yellows']

	print "Parties prediction values:"
	percentages = df.ix[:,4].value_counts(normalize=True,sort=False)
	print percentages



main()
