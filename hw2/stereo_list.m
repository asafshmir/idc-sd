function [ P, error ] = stereo_list( p1, p2, M1, M2 )
%% triangulate:
%       M1 - 3x4 Camera Matrix 1
%       p1 - Nx2 set of points
%       M2 - 3x4 Camera Matrix 2
%       p2 - Nx2 set of points

% Q2.4 - Todo:
%       Implement a triangulation algorithm to compute the 3d locations
%       See Szeliski Chapter 7 for ideas
%
%% Get points
P = [];
one = ones(size(p1,1),1);
p1 = [p1, one];
p2 = [p2, one];
error = 0;
for i = 1:size(p1,1)
    A  = [p1(i,1) * M1(3,:) - M1(1,:);
          p1(i,2) * M1(3,:) - M1(2,:);
          p2(i,1) * M2(3,:) - M2(1,:);
          p2(i,2) * M2(3,:) - M2(2,:)];
    [U, S, V] = svd(A);
    X = V(:, end)';
    X = X / X(size(X,2));
    
    % Get the error
    P1proj = M1*X';
    P2proj = M2*X';
    p_error = sum((p1(i,:)'-P1proj./P1proj(3,:)).^2,1) + ...
        sum((p2(i,:)' - P2proj./P2proj(3,:)).^2,1);
    error = error + p_error;
    
    % Store the new point if error is not too large
    if(p_error < 1000)
        P = [P; X];
    end
end
