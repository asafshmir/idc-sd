function [ P ] = stereo_list( p1, p2, M1, M2 )
% Triangulate a set of 2D coordinates in the image to a set of 3D points
% with the signature
% Inputs:
% M1, M2 - 3*4 camera matrices
% p1, p2 - N*2 matrices with the 2D image coordinates
% Outputs:
% P - N*3 matrix with the corresponding 3D points

M1_trans = M1'*inv((M1*(M1')));
M2_trans = M2'*inv((M2*(M2')));
cop1 = null(M1);
cop2 = null(M2);

in_P = zeros(4, size(p1,2));

for i = 1: size(p1, 2)
    
    p1t = [p1(1,i) p1(2,i) 1]
    p2t = [p2(1,i) p2(2,i) 1]
    
    Pl = M1_trans*p1t';
    Pr = M2_trans*p2t';
    
    ul = Pl-cop1;
    ur = Pr-cop2;
    
    A = [-ul ur];
    B = cop1-cop2;
    lambdas = mldivide(A,B);
   
    in_P(:,i) = lambdas(1)*Pl + (1-lambdas(1))*cop1;
    
end
P = to_homo(in_P);

end

function out_P = to_homo(P)
    out_P = zeros(3,size(P,2));
    for i = 1:size(P,2)
        out_P(:,i) = [P(1)/P(4) P(2)/P(4) P(3)/P(4)];
    end
end

function Y = convert(X)
Y = [0 X(3) -X(2);
       -X(3) 0 X(1);
       X(2) -X(1) 0];
end

