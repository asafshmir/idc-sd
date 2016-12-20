function stereo_algo
    im1=readImage('view5.tif');
    im2=readImage('view1.tif');
    
    patch_width = 1;
    patch_height = 1;
%     disparityRange = [-25 25];
%     disparityMap = ComputeDisparityMap(im1, im2, disparityRange, patch_width, patch_height);
    disparityRange = [0 32];
    disparityMap = disparity(im1, im2, 'BlockSize', 5, 'DisparityRange', disparityRange);
    disparityMap2 = ComputeDisparityMap(im1, im2, disparityRange, 1, 1);

   
    figure
    imshow(disparityMap, disparityRange);
    colorbar
    hold on
end

function D = ComputeDisparityMap(im1, im2, disparityRange, patch_width, patch_height)

    im_size = size(im1,2);

    D = zeros(size(im1,1), size(im1,2));
    for l = patch_width+1:(size(im1, 1)-patch_width) 
        for m = patch_height+1:(size(im1, 2)-patch_height)
           min_disparity = im_size;
           
           %for j = patch_width+1:(im_size-patch_size)
           from_x = max(patch_width+1, m + disparityRange(1));
           to_x = min(im_size-patch_width, m + disparityRange(2));
           for j = from_x:(to_x)
               p1 = [l, m];
               p2 = [l, j];
               disparity = ComputeRectDistance(im1, im2, p1, p2, patch_width, patch_height);       
               if (disparity < min_disparity)
                   min_disparity = disparity;
               end
           end
           D(l, m) = min_disparity;
       end
   end
end

function dist = ComputeRectDistance(im1, im2, p1, p2, patch_width, patch_height)

    x1start = (p1(1)-patch_width);
    x1end = (p1(1)+patch_width);
    y1start = (p1(2)-patch_height);
    y1end = (p1(2)+patch_height);
    
    x2start = (p2(1)-patch_width);
    x2end = (p2(1)+patch_width);
    y2start = (p2(2)-patch_height);
    y2end = (p2(2)+patch_height);
    
    n_pixels = (patch_width*2 + 1)*(patch_height*2 + 1);
    
    vec1 = double(reshape(im1(x1start:x1end, y1start:y1end), 1, n_pixels));
    vec2 = double(reshape(im2(x2start:x2end, y2start:y2end), 1, n_pixels));

    dist = cosine_distance(vec1, vec2);
end

function result = cosine_distance(vec1,vec2)
    result = dot(vec1/norm(vec1), vec2/norm(vec2));
end


