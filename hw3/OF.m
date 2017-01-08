function [u,v] = OF(F1, F2, Smooth, Region)
%OF Summary of this function goes here
%   Detailed explanation goes here
[xx, yy] = meshgrid(-Region:Region, -Region:Region);

Gx = xx .* exp(-(xx .^ 2 + yy .^ 2) / (2 * Smooth ^ 2));
Gy = yy .* exp(-(xx .^ 2 + yy .^ 2) / (2 * Smooth ^ 2));

Ix = conv2(im, Gx, 'same');
Iy = conv2(im, Gy, 'same');


end

