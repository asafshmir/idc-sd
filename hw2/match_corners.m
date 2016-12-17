% Find the corners of two images and match them using 'MatchGrayPatch'
function matching = match_corners(im1,im2,sig_smooth, sig_neighb, lth, density_size, patch_size)
    im1_corners = corners_list(im1, sig_smooth, ...
                                    sig_neighb, lth, density_size, false);
    im2_corners = corners_list(im2, sig_smooth, ...
                                    sig_neighb, lth, density_size, false);
    matching = MatchGrayPatch(im1, im2, im1_corners, im2_corners, patch_size);
end

function matching = MatchGrayPatch(im1,im2,corners1,corners2, patch_size)

Lc = zeros(size(corners1,1),4);

for i=1:size(corners1,1)
    desc_max = 0;
    desc_index = 1;
    
    x1start = (corners1(i,2) - patch_size);
    x1end = (corners1(i,2) + patch_size);
    y1start = (corners1(i,3) - patch_size);
    y1end = (corners1(i,3) + patch_size);
    
    if (y1start > 0 && x1start > 0 && ... 
                x1end < size(im1,1) && y1end < size(im1,1))
            
        for j=1:size(corners2,1)
            x2start = (corners2(j,2) - patch_size);
            x2end = (corners2(j,2) + patch_size);
            y2start = (corners2(j,3) - patch_size);
            y2end = (corners2(j,3) + patch_size);
            
            if (y2start > 0 && x2start > 0 && ... 
                    x2end < size(im2,1) && y2end < size(im2,1))

                vec1 = double(reshape(im1(x1start:x1end, y1start:y1end), ... 
                               1,(patch_size*2+1)^2));
                vec2 = double(reshape(im2(x2start:x2end, y2start:y2end), ... 
                               1,(patch_size*2+1)^2));

                % Compute the descriptor for each corner in each of the images
                % Using dot descriptor
                desc_res = dot_desc(vec1, vec2);
                %desc_res = hist_desc(vec1, vec2);
                
                if (desc_res > desc_max)
                    desc_max = desc_res;
                    desc_index = j;
                    
                end     
            end
        end
        
        % For each corner in im1, find  the nearest neighboor in the list of corners of im2 
        Lc(i,:) = [corners1(i,2),corners1(i,3) ... 
               corners2(desc_index,2),corners2(desc_index,3)];
    end
    
    
end

%plot_correspondence(im1,im2,Lc);
matching = Lc;

end

% Grey level patch, with a similarity measure of a normalized dot vector
function result = dot_desc(vec1,vec2)
    result = dot(vec1/norm(vec1), vec2/norm(vec2));
end

% A histogram of the patch with an angle between the vectors as a similarity measure
function result = hist_desc(vec1,vec2)
    hist1 = hist(vec1);
    hist2 = hist(vec2);
    result = dot(hist1/norm(hist1),hist2/norm(hist2));
end