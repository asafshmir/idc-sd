function stereo_algo
    im1=readImage('view5.tif');
    im2=readImage('view1.tif');
    
    patch_width = 1;
    patch_height = patch_width;
    T = 160;
    f = 1;
    disparityRange = [10 140];
%     disparityMap = ComputeDisparityMap(im1, im2, disparityRange, patch_width, patch_height);
    disparityMap = ComputeOrderedDisparityMap(im1, im2, disparityRange, patch_width, patch_height);
    
    title(['Disparity Map with ', num2str(patch_width*2 + 1),'X', num2str(patch_height*2 + 1), ' patch'])
    depthMap = ComputeDepthMap(T, f, disparityMap);
    figure
    imshow(disparityMap, disparityRange);
    imshow(depthMap,[]);
    colorbar
    hold on
end

function D = ComputeOrderedDisparityMap(im1, im2, disparityRange, patch_height, patch_width)

    im_width = size(im1,2);

    D = zeros(size(im1,1), size(im1,2));
    avg_to_disparity = min(im_width-patch_width+1,disparityRange(2));
    avg_from_disparity = max(patch_width+1,1+ disparityRange(1));
    dist_count = 1;
    for h = patch_height+1:(size(im1, 1)-patch_height) 

        
        
        for l = patch_width+1:(size(im1, 2)-patch_width)
           min_dist = 1;
           min_disparity = im_width;
           
           from_x = max(patch_width+1, l + int16(avg_from_disparity/2));
           to_x = min(im_width-patch_width, l +patch_width+1+int16(avg_to_disparity*1.25));
            
           for r = from_x:(to_x)
               
               p1 = [h, l];
               p2 = [h, r];
               dist = ComputeRectDistance(im1, im2, p1, p2, patch_height, patch_width);       
               
               if (dist < min_dist)
                   min_dist = dist;
                    
                   min_disparity = r - l;
                   dist_count = dist_count + 1;
                   avg_to_disparity = int16((avg_to_disparity * ((dist_count - 1)/dist_count)) + (min_disparity/dist_count));
                   avg_from_disparity = int16((avg_from_disparity * ((dist_count - 1)/dist_count)) + (min_disparity/dist_count));
                   
                   if (min_disparity > 100)
                    disp(avg_from_disparity);
                    disp(min_disparity);
                   end
                   
               end
           end
           
%            disp(avg_disparity);
           D(h, l) = min_disparity;
       end
   end
end


function D = ComputeDisparityMap(im1, im2, disparityRange, patch_height, patch_width)

    im_width = size(im1,2);

    D = zeros(size(im1,1), size(im1,2));
    for h = patch_height+1:(size(im1, 1)-patch_height) 
        for l = patch_width+1:(size(im1, 2)-patch_width)
           min_dist = 1;
           min_disparity = im_width;
           
           from_x = max(patch_width+1, l + disparityRange(1));
           to_x = min(im_width-patch_width, l + disparityRange(2));
           for r = from_x:(to_x)
               p1 = [h, l];
               p2 = [h, r];
               dist = ComputeRectDistance(im1, im2, p1, p2, patch_height, patch_width);       
               if (dist < min_dist)
                   min_dist = dist;
                   min_disparity = r - l;
               end
           end
           D(h, l) = min_disparity;
       end
   end
end

function dist = ComputeRectDistance(im1, im2, p1, p2, patch_height, patch_width)

    x1start = (p1(1)-patch_height);
    x1end = (p1(1)+patch_height);
    y1start = (p1(2)-patch_width);
    y1end = (p1(2)+patch_width);
    
    x2start = (p2(1)-patch_height);
    x2end = (p2(1)+patch_height);
    y2start = (p2(2)-patch_width);
    y2end = (p2(2)+patch_width);
    
    n_pixels = (patch_height*2 + 1)*(patch_width*2 + 1);
    
    vec1 = double(reshape(im1(x1start:x1end, y1start:y1end), 1, n_pixels));
    vec2 = double(reshape(im2(x2start:x2end, y2start:y2end), 1, n_pixels));

    dist = cosine_distance(vec1, vec2); 
    
    % Use negative values so smaller value will represent smaller distance
    % The mininmal value is 1, which means that the vectors have the same
    % direction
    dist = -dist;
end

function result = cosine_distance(vec1,vec2)
    result = dot(vec1/norm(vec1), vec2/norm(vec2));
end

function depthMap = ComputeDepthMap(T, f, disparityMap)
    depthMap = zeros(size(disparityMap,1), size(disparityMap,2))
    for i = 1:size(disparityMap,1)
        for j = 1:size(disparityMap,2)
            depthMap(i,j) = f*T/D(i,j) + 100;
        end
    end
end
