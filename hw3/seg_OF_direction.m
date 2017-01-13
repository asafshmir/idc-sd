function segs = seg_OF_direction( U,V, ths )
%SEG_OF_DIRECTION Summary of this function goes here
%   Detailed explanation goes here
    segs = zeros(size(U,1),size(U,2));
    ths = sort(ths);
    color = 1;
    for b = 1 : size(ths,2) - 1
        color = color + (256/size(ths,2));
        for i = 1 : size(U,1)
            for j = 1 : size(U,2)
                angle = acos(dot(U(i,j),V(i,j)));
                if angle > ths(b) && angle < ths(b+1)
                    segs(i,j) = color; 
                end
                if b == size(ths,2) - 1 && angle > ths(b+1)
                    segs(i,j) = color; 
                end
            end

        end
    end
end