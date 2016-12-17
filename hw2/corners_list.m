function corners = corners_list(im, sig_smooth, sig_neighb, lth, density_size, display) 

epsilon = 0.001;

% Compute the derivatives of the smooth image with a parameter 
% 'sig_smooth' in the x and y direction over the entire image
width = sig_smooth * 3;
[xx, yy] = meshgrid(-width:width, -width:width);

Gx = xx .* exp(-(xx .^ 2 + yy .^ 2) / (2 * sig_smooth ^ 2));
Gy = yy .* exp(-(xx .^ 2 + yy .^ 2) / (2 * sig_smooth ^ 2));

Ix = conv2(im, Gx, 'same');
Iy = conv2(im, Gy, 'same');

% Compute the following three matrices: Ix2, Iy2, IxIy 
Ix2 = Ix .^ 2;
Iy2 = Iy .^ 2;
IxIy = Ix .* Iy;

% Compute the eigen values, lambda_small and lambda_large using the 
% matrix C. 
% Save the values lambda_small for all pixles in a matrix, Lsmall
Gxy = exp(-(xx .^ 2 + yy .^ 2) / (2 * sig_neighb ^ 2));
Sx2 = conv2(Ix2, Gxy, 'same');
Sy2 = conv2(Iy2, Gxy, 'same');
Sxy = conv2(IxIy, Gxy, 'same');

num_rows = size(im, 1);
num_columns = size(im, 2);

Lsmall = zeros(num_rows, num_columns);
im_eigen_vecs = zeros(num_rows, num_columns, 2);
for x=1:num_rows,
   for y=1:num_columns,
       % Compute C
       C = [Sx2(x, y) Sxy(x, y); Sxy(x, y) Sy2(x, y)];
       
       [eigen_vecs_mat, eigen_vals_mat] = eig(C, 'matrix');       
       eigen_vals = diag(eigen_vals_mat);
       lambda_small = eigen_vals(1);
       im_eigen_vecs(x,y,:) = eigen_vecs_mat(1,:);

       Lsmall(x, y) = lambda_small;
   end
end

% Threshold the small eignevalue, lambda_small using the parameter lth
% and set to zero those pixles that are below the threshold
Lsmall_thresholded = zeros(num_rows, num_columns);
for x=1:num_rows,
   for y=1:num_columns,
     
       if (Lsmall(x, y) > lth)
          Lsmall_thresholded(x, y) = Lsmall(x, y); 
       end       
   end
end

% For each region of size 'denisty_size', leave the maximal lambda_small value
% Construct a list L with lambda_small values that are higher than lth
[row, col, L] = find(Lsmall_thresholded);
% Sort L 
[L_sorted, b] = sort(L, 'descend');
row = row(b);
col = col(b);

for i=1:size(L_sorted,1),        
    for j=i:size(L_sorted,1)
        delta_y = abs(row(i) - row(j));
        delta_x = abs(col(i) - col(j));

        % Delete all points from L in the neighborhood of p
        if (L_sorted(i) > L_sorted(j) + epsilon && ...
            delta_x < density_size && delta_y < density_size)      
            L_sorted(j) = 0; row(j) = 0; col(j) = 0;
        end
    end 
end

L_sorted = L_sorted(L_sorted ~= 0);
row = row(row ~= 0);
col = col(col ~= 0);

% Return 'corners_list'
corners = ones(size(col, 1), 5);
for i=1:size(corners,1)
    corners(i, 1) = L_sorted(i);  % Eigen value
    corners(i, 2) = col(i); % X coordinate
    corners(i, 3) = row(i); % Y coordinate
    corners(i, 4) = im_eigen_vecs(row(i),col(i), 1); % Eigen vector
    corners(i, 5) = im_eigen_vecs(row(i),col(i), 2); % Eigen vector
end

% If 'display' = 1, then display as part of the function
if (display)
    % The original image
    figure,imshow(im);
    fig_title = sprintf('original');
    title(fig_title);
    hold on
    % The lambda_small image before threshold 
    figure,imshow(Lsmall);
    fig_title = sprintf('lambda small before threshold');
    title(fig_title);
    hold on
    % The lambda_small image after threshold 
    figure,imshow(Lsmall_thresholded);
    fig_title = sprintf('lambda small after threshold');
    title(fig_title);
    hold on
    % The detected corners overlay on the original image
    figure,imshow(im);
    hold on
    fig_title = sprintf(['sig smooth: %d, sig neighb: %d, '  ... 
        'lth: %d, density size: %d'], ...
        sig_smooth, sig_neighb, lth, density_size);
    title(fig_title);
    hold on
    plot(corners(:,2), corners(:,3), 'r*');
end

end 



