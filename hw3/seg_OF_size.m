function [background, foreground] = seg_OF_size( U,V, th )
%SEG_OF_SIZE Summary of this function goes here
%   Detailed explanation goes here
background = zeros(size(U,1), size(U,2));
foreground = zeros(size(U,1), size(U,2));
backColor = randi(256);
foreColor = randi(256);
for i = 1 : size(U,1)
    for j = 1 : size(U,2)
        value = U(i,j)^2 + V(i,j)^2;
        if value > th 
            foreground(i,j) = backColor; 
        else
            background(i,j) = foreColor;
        end
    end
end
