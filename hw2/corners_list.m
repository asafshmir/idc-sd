function corners = corners_list(im, sig_smooth, sig_neighb, lth, density_size, display) 

numOfRows = size(im, 1);
numOfColumns = size(im, 2);

% 1) Compute x and y derivatives of image using sig_smooth
w = 3;
[xx, yy] = meshgrid(-w:w, -w:w);

Gx = xx .* exp(-(xx .^ 2 + yy .^ 2) / (2 * sig_smooth ^ 2));
Gy = yy .* exp(-(xx .^ 2 + yy .^ 2) / (2 * sig_smooth ^ 2));

Ix = conv2(im, Gx, 'same');
Iy = conv2(im, Gy, 'same');

% 2) Compute the following three matrices, Ix2, Iy2, IxIy 
Ix2 = Ix .^ 2;
Iy2 = Iy .^ 2;
IxIy = Ix .* Iy;

% 3) Define the basis for matrix C - convolution with guassian of 
%    Ix2, Iy2, IxIy using sig_neighb as parameter
Gxy = exp(-(xx .^ 2 + yy .^ 2) / (2 * sig_neighb ^ 2));
Sx2 = conv2(Ix2, Gxy, 'same');
Sy2 = conv2(Iy2, Gxy, 'same');
Sxy = conv2(IxIy, Gxy, 'same');

im_before_threshold = zeros(numOfRows, numOfColumns);
im_after_threshold = zeros(numOfRows, numOfColumns);
im_eigen_vecs = zeros(numOfRows, numOfColumns, 2);
for x=1:numOfRows,
   for y=1:numOfColumns,
       % Compute the matrix C for the 
       C = [Sx2(x, y) Sxy(x, y); Sxy(x, y) Sy2(x, y)];
       
       % 4) Compute the Eigenvalues & Eigenvectors and save the smaller Eigenvalue
       [eigen_vecs, eigen_vals] = eig(C, 'matrix');       
       dia = diag(eigen_vals);
       lsmall = dia(1);
       im_eigen_vecs(x,y,:) = eigen_vecs(1,:);
       
       % 5) Threshold the small Eignevalue
       im_before_threshold(x, y) = lsmall;
       if (lsmall > lth)
          im_after_threshold(x, y) = lsmall; 
       end       
   end
end

 % 6) For each region of size  denisty_size, leave the maximal lsmall
[row, col, v] = find(im_after_threshold);

% Sort the lsmall values
[sorted_v, b] = sort(v, 'descend');
row = row(b);
col = col(b);

% Iterate over all lsmall values in decsending order, 
% look for other points in the area (in density_size),
% and ignore these points (we're looking for local maximums)
for i=1:size(sorted_v,1),        
    for j=i:size(sorted_v,1)
        % The area for computing the local maximum
        delta_y = abs(row(i) - row(j));
        delta_x = abs(col(i) - col(j));

        if (sorted_v(i) > sorted_v(j) + 0.001 && ...
            delta_x < density_size && delta_y < density_size)      
            sorted_v(j) = 0; row(j) = 0; col(j) = 0;
        end
    end 
end
% Remove all ignored pixels
sorted_v = sorted_v(sorted_v ~= 0);
row = row(row ~= 0);
col = col(col ~= 0);

% 7) Return, corners_list:
%   1: The smaller eigenvalue
%   2: The x value of the corner
%   3: The y value of the corner
%   4+5: The 2X1 eigenvector corresponding to the eigenvalue
corners = ones(size(col, 1), 5);
for i=1:size(corners,1)
    corners(i, 1) = sorted_v(i);  % The eigen values
    corners(i, 2) = col(i); % The x coordinates
    corners(i, 3) = row(i); % The y coordinates
    corners(i, 4) = im_eigen_vecs(row(i),col(i), 1); % The eigen vector
    corners(i, 5) = im_eigen_vecs(row(i),col(i), 2); % The eigen vector
end

% 8) Display the lsmall image before and after threshold
%    Display the original image with the detected corners
if (display)
    figure,imshow(im_before_threshold);
    hold on
    figure,imshow(im_after_threshold);
    hold on
    
    figure,imshow(im);
    hold on
    my_title = sprintf(['sig-smooth: %d, sig-neigh: %d, '  ... 
        'threshold: %d, density-size: %d'], ...
        sig_smooth, sig_neighb, lth, density_size);
    title(my_title);
    hold on
    plot(corners(:,2), corners(:,3), 'r*');
end

end 



