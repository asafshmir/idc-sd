function [u,v] = OF(F1, F2, Smooth, Region)
%OF Summary of this function goes here
%   Detailed explanation goes here
[xx, yy] = meshgrid(-Region:Region, -Region:Region);

Gx = xx .* exp(-(xx .^ 2 + yy .^ 2) / (2 * Smooth ^ 2));
Gy = yy .* exp(-(xx .^ 2 + yy .^ 2) / (2 * Smooth ^ 2));
Gt = yy .* exp(-(xx .^ 2 + yy .^ 2) / (2 * Smooth ^ 2));

Ix = conv2(F1, Gx, 'same');
Iy = conv2(F1, Gy, 'same');
It = F2-F1;

A = size(Region, Region);
b = It;
for i = 1:Region
    for j = 1:Region
        A(i,j) = [Ix(i,j)/It(i,j); Iy(i,j)/It(i,j)]; 
    end
end
Ainv = pinv(A);

end

