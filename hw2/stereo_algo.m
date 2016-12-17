function main
    im1=readImage('view1.tif');
    im2=readImage('view5.tif');
    ps1 = [1 1 1
           1 1 1];
    ps2 = [1 1 1
           1 1 1];
%     str_algo(im1, im2, ps1, ps2, 10);
    MatchGrayPatch(im1, im2, ps1, ps2, 10);
end

function str = str_algo(im1, im2, ps1, ps2, patch_size)

    minimal_distance = 0;
    min_point1 = [0 0];
    min_point2 = [0 0];
    
    s = size(ps1,1);
%     disp(size(ps1,1));
%     D = zeros(size(ps1,2));
    D = zeros(4);
    for i = 1:size(ps1,2)
        rect1 = get_rectangle(ps1(i), patch_size);
        rect2 = get_rectangle(ps2(i), patch_size);

        vec1 = get_vector(im1, rect1, patch_size);
        vec2 = get_vector(im2, rect2, patch_size);
        distance = cosine_distance(vec1, vec2);

        if minimal_distance == 0
           minimal_distance = distance; 
           min_point1 = ps1(i);
           min_point2 = ps2(i);
        elseif distance < minimal_distance
           minimal_distance = distance;
           min_point1 = ps1(i);
           min_point2 = ps2(i);
        end

        D(i) = distance 

    end
    str = D
end


function MatchGrayPatch(im1,im2,corners1,corners2, patch_size)

MatchGrayPatch = zeros(size(corners1,1));

for i=1:size(corners1,1)
    desc1_max = 0;
    desc2_max = 0;
    desc1_min = 0;
    desc2_min = 0;
    desc1_index = 1;
    desc2_index = 1;
    
    x1start = (corners1(i,2)-patch_size);
    x1end = (corners1(i,2)+patch_size);
    y1start = (corners1(i,3)-patch_size);
    y1end = (corners1(i,3)+patch_size);
    
    if (y1start > 0 && x1start > 0 && ... 
                x1end < size(im1,1) && y1end < size(im1,1))
            
        for j=1:size(corners2,1)
            x2start = (corners2(j,2)-patch_size);
            x2end = (corners2(j,2)+patch_size);
            y2start = (corners2(j,3)-patch_size);
            y2end = (corners2(j,3)+patch_size);
            
            if (y2start > 0 && x2start > 0 && ... 
                    x2end < size(im2,1) && y2end < size(im2,1))

                vec1 = double(reshape(im1(x1start:x1end, y1start:y1end), ... 
                               1,(patch_size*2+1)^2));
                vec2 = double(reshape(im2(x2start:x2end, y2start:y2end), ... 
                               1,(patch_size*2+1)^2));

                desc1_ret = descriptor1(vec1,vec2);
                desc2_ret = descriptor2(vec1,vec2);
                
%                 if (desc1_ret > desc1_max)
%                     desc1_max = desc1_ret;
%                     desc1_index = j;
%                 end
                if (desc1_ret < desc1_min)
                    desc1_min = desc1_ret;
                    desc1_index = j;
                end
%                 if (desc2_ret > desc2_max)
%                     desc2_max = desc2_ret;
%                     desc2_index = j;
%                 end                
            end
        end
        MatchGrayPatch(i) = desc1_min + 100;
    end
    
    
end

end

function rect = get_rectangle(p, patch_size)
 rect = [(p(1) - patch_size(1)/2) (p(2) - patch_size(1)/2);
         (p(1) - patch_size(1)/2) (p(2) + patch_size(1)/2);
         (p(1) + patch_size(2)/2) (p(2) - patch_size(2)/2);
         (p(1) + patch_size(2)/2) (p(2) + patch_size(2)/2);];
end


function vector = get_vector(im, rect, patch_size)
    vector = double(reshape(im(rect(1):rect(2), rect(3):rect(4)), ... 
                               1,(patch_size+1)^2));
end
function result = cosine_distance(vec1,vec2)
    result = dot(vec1/norm(vec1), vec2/norm(vec2));
end


