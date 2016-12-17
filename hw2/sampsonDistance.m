
% This function computes the Sampson distance for a list of matches
% Input: F - the 3X3 fundamental matrix
%        x1 - 2XN matrix of features (first row X, second row Y) in the first image
%        where N is the number of features
%        x2 - 2XN matrix of features in the second image in the same format
%        as x1
function distVec = sampsonDistance(F, x1, x2)
   
    N = size(x1,2);
    x1 = [x1 ; ones(1,N)];
    x2 = [x2 ; ones(1,N)];
    
   	x2tFx1 = zeros(1,length(x1));
	for n = 1:length(x1)
	    x2tFx1(n) = x2(:,n)'*F*x1(:,n);
	end
	
	Fx1 = F*x1;
	Ftx2 = F'*x2;     
	
	% Evaluate distances
	distVec =  x2tFx1.^2 ./ ...
	     (Fx1(1,:).^2 + Fx1(2,:).^2 + Ftx2(1,:).^2 + Ftx2(2,:).^2);
     
    distVec = sqrt(distVec);
	
end
    
    
    