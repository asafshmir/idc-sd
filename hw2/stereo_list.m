function [ P ] = stereo_list( p1, p2, M1, M2 )
% Triangulate a set of 2D coordinates in the image to a set of 3D points
% with the signature
% Inputs:
% M1, M2 - 3*4 camera matrices
% p1, p2 - N*2 matrices with the 2D image coordinates
% Outputs:
% P - N*3 matrix with the corresponding 3D points

    M1pinv = pinv(M1);
    M2pinv = pinv(M2);
    cop1 = null(M1, 'r');
    cop2 = null(M2, 'r');

    in_P = [];

    for i = 1: size(p1, 1)
        % The given 2D points
        p1i = [p1(i,1:2) 1];
        p2i = [p2(i,1:2) 1];
        % The projected points on the two images
        Sl = to_non_homo(M1pinv*p1i');
        Sr = to_non_homo(M2pinv*p2i');
        % The COPs
        cL = to_non_homo(cop1);
        cR = to_non_homo(cop2);
        % The projection rays
        ul = Sl-cL;
        ur = Sr-cR;

        % Solve Ax=b in order to find lambdas 
        A = [-ul ur];
        b = cL - cR;
        lambdas = A\b;

        % The mean of the two projected points (each camera gives a different
        % lambda)
        in_P1 = cL + lambdas(1)*ul;
        in_P2 = cR + lambdas(2)*ur;
        A = [in_P1, in_P2];
        averagePoint = mean(A, 2);

        in_P = [in_P ; averagePoint'];
    end
    P = in_P;
end

% Convert a given set of 3D homogenous points to non-homogenous
function out_P = to_non_homo(P)
    out_P = zeros(3, size(P, 2));
    for i = 1:size(P,2)
        if (P(4,i) ~= 0)
            out_P(:,i) = [P(1,i)/P(4,i) P(2,i)/P(4,i) P(3,i)/P(4,i)];
        else
            out_P(:,i) = [P(1,i) P(2,i) P(3,i)];
        end
    end
end

