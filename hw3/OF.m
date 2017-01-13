function [U,V] = OF(F1, F2, Smooth, Region)
    %OF Summary of this function goes here
    %   Detailed explanation goes here
    [xx, yy] = meshgrid(-Region:Region, -Region:Region);

    Gx = xx .* exp(-(xx .^ 2 + yy .^ 2) / (2 * Smooth ^ 2));
    Gy = yy .* exp(-(xx .^ 2 + yy .^ 2) / (2 * Smooth ^ 2));

    Ix = conv2(double(F1), Gx, 'same');
    Iy = conv2(double(F1), Gy, 'same');
    It = double(F2-F1);

    U = zeros(size(F1,1),size(F1,2));
    V = zeros(size(F1,1),size(F1,2));
    
%     A = [Ix, Iy];    
    b = -It;
    msize = size(b,1)*size(b,2)
    b = reshape(b,msize,1);
    Ix = reshape(Ix,msize,1);
    Iy = reshape(Iy,msize,1);
    A = [Ix Iy];
%   
%     for i = 1:msize
%         
%             A(i,1) = Ix(i,1);
%             A(i,2) = Iy(i,2);
%             b = -It(i,j);
%             G = A'*A;
%             r = rank(G);
% 
%             if r >= 1
%                 Ainv = pinv(A);
%                 U(i,j) = b*Ainv;
%                 V(i,j) = b*Ainv*b(2);
%             else
%                 U(i,j) = 0;
%                 V(i,j) = 0;
%             end  
        
%     end
    
    G = A'*A;
    if rank(G) >= 2
        Ainv = pinv(A);
        result = Ainv * b;
    end

end


