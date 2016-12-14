% function result = stereo_list(points_i1, points_i2, P1, P2)
% result = zeros(4, size(points_i1, 2));
% for i=1:size(points_i1,2)
%     % select the point from each image
%     point_i1 = points_i1(:,i);
%     point_i2 = points_i2(:,i);
% 
%     % triangulate 3D point
%     result(:,i) = triangulatePoint(point_i1, point_i2, P1, P2);
% end
% 
% % homogeneous coordinates
% result = result./repmat(result(4,:),4,1);
% 
% function point = triangulatePoint(point_i1, point_i2, P1, P2)
% % compute the matrix A
% A = [...
%     P1(3,:) * point_i1(1) - P1(1,:);
%     P1(3,:) * point_i1(2) - P1(2,:);
%     P2(3,:) * point_i2(1) - P2(1,:);
%     P2(3,:) * point_i2(2) - P2(2,:)];
% 
% % normalise A
% for p = 1:4
%     A(p,:) = A(p,:)/norm(A(p,:));
% end
% 
% % compute the 3D point
% [~, ~, point] = svd(A);
% point = point(:, end);
% 
% point = point / point(end);

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
function [ P ] = stereo_list( p1, p2, M1, M2 )
% Triangulate a set of 2D coordinates in the image to a set of 3D points
% with the signature
% Inputs:
% M1, M2 - 3*4 camera matrices
% p1, p2 - N*2 matrices with the 2D image coordinates
% Outputs:
% P - N*3 matrix with the corresponding 3D points

% convert to homogenous
p1 = [ p1'; ones(1, size(p1, 1))];
p2 = [ p2'; ones(1, size(p2, 1))];

P = zeros(4, size(p1,2));

for i = 1: size(p1, 2)
    p = convert(p1(:, i));
    q = convert(p2(:, i));
    
    T = [p*M1; q*M2];
    
    % do SVD
    [~, ~, V] = svd(T);
    vtemp = V(:, end);
    P(:, i) = vtemp/vtemp(4);
end

P = P';
P = P(:, 1:3);
end

function Y = convert(X)
Y = [0 X(3) -X(2);
       -X(3) 0 X(1);
       X(2) -X(1) 0];
end

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% function [ P, error ] = stereo_list( p1, p2, M1, M2 )
% %% triangulate:
% %       M1 - 3x4 Camera Matrix 1
% %       p1 - Nx2 set of points
% %       M2 - 3x4 Camera Matrix 2
% %       p2 - Nx2 set of points
% 
% % Q2.4 - Todo:
% %       Implement a triangulation algorithm to compute the 3d locations
% %       See Szeliski Chapter 7 for ideas
% %
% %% Get points
% P = [];
% one = ones(size(p1,1),1);
% p1 = [p1, one];
% p2 = [p2, one];
% error = 0;
% for i = 1:size(p1,1)
%     A  = [p1(i,1) * M1(3,:) - M1(1,:);
%           p1(i,2) * M1(3,:) - M1(2,:);
%           p2(i,1) * M2(3,:) - M2(1,:);
%           p2(i,2) * M2(3,:) - M2(2,:)];
%     [U, S, V] = svd(A);
%     X = V(:, end)';
%     X = X / X(size(X,2));
%     
%     % Get the error
%     P1proj = M1*X';
%     P2proj = M2*X';
%     p_error = sum((p1(i,:)'-P1proj./P1proj(3,:)).^2,1) + ...
%         sum((p2(i,:)' - P2proj./P2proj(3,:)).^2,1);
%     error = error + p_error;
%     
%     % Store the new point if error is not too large
%     if(p_error < 1000)
%         P = [P; X];
%     end
% end


%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% 
% function [XL,XR] = stereo_list(ps1,ps2,om,T,fc_left,cc_left,kc_left,alpha_c_left,fc_right,cc_right,kc_right,alpha_c_right),
% 
% % [XL,XR] = stereo_triangulation(xL,xR,om,T,fc_left,cc_left,kc_left,alpha_c_left,fc_right,cc_right,kc_right,alpha_c_right),
% %
% % Function that computes the position of a set on N points given the left and right image projections.
% % The cameras are assumed to be calibrated, intrinsically, and extrinsically.
% %
% % Input:
% %           ps1: 2xN matrix of pixel coordinates in the left image
% %           ps2: 2xN matrix of pixel coordinates in the right image
% %           om,T: rotation vector and translation vector between right and left cameras (output of stereo calibration)
% %           fc_left,cc_left,...: intrinsic parameters of the left camera  (output of stereo calibration)
% %           fc_right,cc_right,...: intrinsic parameters of the right camera (output of stereo calibration)
% %
% % Output:
% %
% %           XL: 3xN matrix of coordinates of the points in the left camera reference frame
% %           XR: 3xN matrix of coordinates of the points in the right camera reference frame
% %
% % Note: XR and XL are related to each other through the rigid motion equation: XR = R * XL + T, where R = rodrigues(om)
% % For more information, visit http://www.vision.caltech.edu/bouguetj/calib_doc/htmls/example5.html
% %
% %
% % (c) Jean-Yves Bouguet - Intel Corporation - April 9th, 2003
% 
% 
% 
% %--- Normalize the image projection according to the intrinsic parameters of the left and right cameras
% xt = normalize_pixel(ps1,fc_left,cc_left,kc_left,alpha_c_left);
% xtt = normalize_pixel(ps2,fc_right,cc_right,kc_right,alpha_c_right);
% 
% %--- Extend the normalized projections in homogeneous coordinates
% xt = [xt;ones(1,size(xt,2))];
% xtt = [xtt;ones(1,size(xtt,2))];
% 
% %--- Number of points:
% N = size(xt,2);
% 
% %--- Rotation matrix corresponding to the rigid motion between left and right cameras:
% R = rodrigues(om);
% 
% 
% %--- Triangulation of the rays in 3D space:
% 
% u = R * xt;
% 
% n_xt2 = dot(xt,xt);
% n_xtt2 = dot(xtt,xtt);
% 
% T_vect = repmat(T, [1 N]);
% 
% DD = n_xt2 .* n_xtt2 - dot(u,xtt).^2;
% 
% dot_uT = dot(u,T_vect);
% dot_xttT = dot(xtt,T_vect);
% dot_xttu = dot(u,xtt);
% 
% NN1 = dot_xttu.*dot_xttT - n_xtt2 .* dot_uT;
% NN2 = n_xt2.*dot_xttT - dot_uT.*dot_xttu;
% 
% Zt = NN1./DD;
% Ztt = NN2./DD;
% 
% X1 = xt .* repmat(Zt,[3 1]);
% X2 = R'*(xtt.*repmat(Ztt,[3,1])  - T_vect);
% 
% 
% %--- Left coordinates:
% XL = 1/2 * (X1 + X2);
% 
% %--- Right coordinates:
% XR = R*XL + T_vect;