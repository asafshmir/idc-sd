function main
    im1=readImage('view1.tif');
    im2=readImage('view5.tif');
    im_size = size(im1,2);
    patch_size = 1;
    max_shift_x = 15;
    
    D = zeros(size(im1,1), size(im1,2));
    for l = patch_size+1:(size(im1, 1)-patch_size) 
        for m = patch_size+1:(size(im1, 2)-patch_size)
           min_disparity = 0;
           
           %for j = patch_size+1:(im_size-patch_size)
           from_x = max(patch_size+1, m - max_shift_x);
           to_x = min(im_size-patch_size, m + max_shift_x);
           for j = from_x:(to_x)
               p1 = [l, m];
               p2 = [l, j];
               disparity = ComputeRectDistance(im1, im2, p1, p2, patch_size);       
               if (disparity < min_disparity)
                   min_disparity = disparity;
               end
           end
           D(l, m) = min_disparity;
       end
   end
   imshow(D, []);
    
end

function ComputeRectDistance = ComputeRectDistance(im1,im2,p1,p2, patch_size)

    x1start = (p1(1)-patch_size);
    x1end = (p1(1)+patch_size);
    y1start = (p1(2)-patch_size);
    y1end = (p1(2)+patch_size);
    
    x2start = (p2(1)-patch_size);
    x2end = (p2(1)+patch_size);
    y2start = (p2(2)-patch_size);
    y2end = (p2(2)+patch_size);
    
    vec1 = double(reshape(im1(x1start:x1end, y1start:y1end), 1, (patch_size*2 + 1)^2));
    vec2 = double(reshape(im2(x2start:x2end, y2start:y2end), 1, (patch_size*2 + 1)^2));

    ComputeRectDistance = cosine_distance(vec1, vec2);
end

function result = cosine_distance(vec1,vec2)
    result = dot(vec1/norm(vec1), vec2/norm(vec2));
end


