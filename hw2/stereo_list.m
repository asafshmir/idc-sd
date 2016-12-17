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

% in_P = zeros(4, size(p1,2));
in_P = [];

for i = 1: size(p1, 1)
    
    p1i = [p1(i,1:2) 1];
    p2i = [p2(i,1:2) 1];
    
    Pl = to_inhomo(M1pinv*p1i');
    Pr = to_inhomo(M2pinv*p2i');
    cL = to_inhomo(cop1);
    cR = to_inhomo(cop2);
    ul = Pl-cL;
    ur = Pr-cR;
    
    A = [-ul ur];
    b = cL - cR;
    lambdas = A\b;
   
    in_P1 = cL + lambdas(1)*ul;
    in_P2 = cR + lambdas(2)*ur;
    A = [in_P1, in_P2];
    meanA = mean(A,2);

    in_P = [in_P ; meanA'];
end
% P = to_homo(in_P);
P = in_P;

end

function out_P = to_inhomo(P)
    out_P = zeros(3,size(P,2));
    for i = 1:size(P,2)
        if (P(4,i) ~= 0)
            out_P(:,i) = [P(1,i)/P(4,i) P(2,i)/P(4,i) P(3,i)/P(4,i)];
        else
            out_P(:,i) = [P(1,i) P(2,i) P(3,i)];
        end
    end
end

