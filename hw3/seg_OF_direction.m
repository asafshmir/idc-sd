function segs = seg_OF_direction( U,V, ths )
%SEG_OF_DIRECTION Summary of this function goes here
%   Detailed explanation goes here
    segs = zeros(size(U,1),size(U,2));
    ths = sort(ths);
    
    color = 0;
    colors = size(ths,2)-1;
%     colors = zeros(size(ths));
%     for i = 1 : size(colors)
%         colors(i) = color;
%         color = color + 255 / size(colors); 
%     end
    
    for c = 1 : colors
        %color = color + (256/size(ths,2));
        color = color + 255 / colors; 
        for i = 1 : size(U,1)
            for j = 1 : size(U,2)
                %angle = acos(dot(U(i,j),V(i,j)));
                %angle = atan2(norm(cross(v1,v2)),dot(v1,v2))
                angle = radtodeg(atan(V(i,j)/U(i,j)));
                if(angle < 0)
                    angle = angle + 360;
                end
                if angle > ths(c) && angle < ths(c+1)
                    segs(i,j) = color; 
                end
%                 if c == size(ths,2) - 1 && angle > ths(c+1)
%                     segs(i,j) = color; 
%                 end
            end

        end
    end
end