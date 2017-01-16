function segs = seg_OF_direction( U,V, ths )
%SEG_OF_DIRECTION Summary of this function goes here
%   Detailed explanation goes here
bins = zeros(size(ths,1),size(U,1),size(U,2));
ths = sort(ths)
for b = 1 : size(bins,1)
    for i = 1 : size(U,1)
        for j = 1 : size(U,2)
            angle = acos(dot(U(i,j),V(i,j)));
            if angle > ths(b)
                bins(b,i,j) = value; 
            end
        end

    end
end
end

