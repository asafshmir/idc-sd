function stereo_algo
    % Read the images for the stereo algorithm
    im1=readImage('view5.tif');
    im2=readImage('view1.tif');
    
    % Algorithm parameters
    patch_width = 5;
    patch_height = patch_width;
    T = 160;
    f = 1;
    disparityRange = [10 140];
    
    % Compute and display the disparity map with the assumption that the
    % order is preserved
    disparityMap = ComputeOrderedDisparityMap(im1, im2, disparityRange, patch_width, patch_height);

    figure;
    imshow(disparityMap, disparityRange);
    colorbar;
    hold on;
    f1 = gcf;
    figure(f1);
    title(['Disparity Map (with order assumption), ', num2str(patch_width*2 + 1),'X', num2str(patch_height*2 + 1), ' patch']);
    
    % Compute and display the depth map from the disparity map
    % Use the given T and f
    depthMap = ComputeDepthMap(T, f, disparityMap);
    
    figure;
    imshow(depthMap,[]);
    hold on;
    f2 = gcf;
    figure(f2);
    title('Depth Map (with order assumption)');
    
    % Compute and display the disparity map without the assumption that the
    % order is preserved
    patch_width = 1;
    patch_height = patch_width;
    disparityMap = ComputeDisparityMap(im1, im2, disparityRange, patch_width, patch_height);
    
    figure;
    imshow(disparityMap, disparityRange);
    colorbar;
    hold on;
    f3 = gcf;
    figure(f3);
    title(['Disparity Map (without order assumption), ', num2str(patch_width*2 + 1),'X', num2str(patch_height*2 + 1), ' patch'])
    
    % Compute and display the depth map from the disparity map
    % Use the given T and f
    depthMap = ComputeDepthMap(T, f, disparityMap);
    
    figure;
    imshow(depthMap,[]);
    hold on;
    f4 = gcf;
    figure(f4);
    title('Depth Map (without order assumption)');
    
    % The absolute value of the differences between im1 and the
    % corresponded values in im2 (according to the disparity map)
    diffMap = ComputeDifferenceMap(im1, im2, disparityMap);
    
    figure;
    imshow(diffMap,[]);
    hold on;
    f5 = gcf;
    figure(f5);
    title('Diff Map (without order assumption)');

end

% Compute the absolute difference between each pixel from im1 to its
% corresponded pixel from im2. The corresponded pixel is determined by the
% given disparity map
function D = ComputeDifferenceMap(im1, im2, disparityMap)
    im_width = size(im1,2);
    im_height = size(im1,1);
    D = zeros(im_height, im_width);
    
    for h = 1:im_height
        for l = 1:im_width
            cur_disp = disparityMap(h, l);
            if (l + cur_disp <= im_width)
                D(h, l) = abs(im1(h, l) - im2(h, l + cur_disp));
            else
                D(h, l) = 255;
            end
        end
    end
end

% Compute the disparity map of two given images with a similarity mesure 
% of cosine distance between patches in a given size.
% Assume assumption that the order of points is preserved.
function D = ComputeOrderedDisparityMap(im1, im2, disparityRange, patch_height, patch_width)

    im_width = size(im1,2);

    D = zeros(size(im1,1), size(im1,2));
    % Iterate over all the pixels of the first image
    for h = patch_height+1:(size(im1, 1)-patch_height) 
        for l = patch_width+1:(size(im1, 2)-patch_width)
            min_dist = 1;
            min_disparity = im_width;
            % Use the neighbor's disparity as the minimal disparity because
            % of the order assumption
            neighbor_disparity = disparityRange(1);
            if (l > patch_width + 1)
                neighbor_disparity = D(h, l-1);
            end

            % Iterate over all pixels from neighbor's disparity to upper
            % bound of disparity range
            from_x = max(patch_width+1, l + neighbor_disparity);
            to_x = min(im_width-patch_width, l + disparityRange(2));
            for r = from_x:(to_x)
                p1 = [h, l];
                p2 = [h, r];
                % Compute the distance between the patches around pixel from
                % im1 and corresponding pixel from im2
                dist = ComputeRectDistance(im1, im2, p1, p2, patch_height, patch_width);       
                % If a new minimal distance is found, store the disparity
                if (dist < min_dist)
                    min_dist = dist;
                    min_disparity = r - l;
                end
            end
            D(h, l) = min_disparity;
        end
    end
end

% Compute the disparity map of two given images with a similarity mesure 
% of cosine distance between patches in a given size.
% Don't assume assumption that the order of points is preserved.
function D = ComputeDisparityMap(im1, im2, disparityRange, patch_height, patch_width)

    im_width = size(im1,2);

    D = zeros(size(im1,1), size(im1,2));
    % Iterate over all the pixels of the first image
    for h = patch_height+1:(size(im1, 1)-patch_height) 
        for l = patch_width+1:(size(im1, 2)-patch_width)
           min_dist = 1;
           min_disparity = im_width;
           
           % Iterate over all pixels in the given range
           from_x = max(patch_width+1, l + disparityRange(1));
           to_x = min(im_width-patch_width, l + disparityRange(2));
           for r = from_x:(to_x)
               p1 = [h, l];
               p2 = [h, r];
               % Compute the distance between the patches around pixel from
               % im1 and corresponding pixel from im2
               dist = ComputeRectDistance(im1, im2, p1, p2, patch_height, patch_width);       
               % If a new minimal distance is found, store the disparity
               if (dist < min_dist)
                   min_dist = dist;
                   min_disparity = r - l;
               end
           end
           D(h, l) = min_disparity;
       end
   end
end

% Compute the disatance between two given patches
function dist = ComputeRectDistance(im1, im2, p1, p2, patch_height, patch_width)

    % Patch border in first image
    x1start = (p1(1)-patch_height);
    x1end = (p1(1)+patch_height);
    y1start = (p1(2)-patch_width);
    y1end = (p1(2)+patch_width);
    
    % Patch border in second image
    x2start = (p2(1)-patch_height);
    x2end = (p2(1)+patch_height);
    y2start = (p2(2)-patch_width);
    y2end = (p2(2)+patch_width);
    
    % Patch size (number of pixels)
    n_pixels = (patch_height*2 + 1)*(patch_width*2 + 1);
    
    % Reshape the patchs (sum-matrices) into vectors
    vec1 = double(reshape(im1(x1start:x1end, y1start:y1end), 1, n_pixels));
    vec2 = double(reshape(im2(x2start:x2end, y2start:y2end), 1, n_pixels));

    % Compute the cosine distance between the patches
    dist = cosine_distance(vec1, vec2); 
    
    % Use negative values so smaller value will represent smaller distance
    % The mininmal value is 1, which means that the vectors have the same
    % direction
    dist = -dist;
end

% Compute the cosine distance between two given vectors
function result = cosine_distance(vec1,vec2)
    result = dot(vec1/norm(vec1), vec2/norm(vec2));
end

% Compute the depth map using a given disparity map and the camera
% parameters
function depthMap = ComputeDepthMap(T, f, disparityMap)
    depthMap = zeros(size(disparityMap,1), size(disparityMap,2));
    for i = 1:size(disparityMap,1)
        for j = 1:size(disparityMap,2)
            depthMap(i,j) = f*T/disparityMap(i,j) + 100;
        end
    end
end
