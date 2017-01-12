function [U,V] = OF(F1, F2, Smooth, Region)
    %OF Summary of this function goes here
    %   Detailed explanation goes here
    [xx, yy] = meshgrid(-Region:Region, -Region:Region);

    Gx = xx .* exp(-(xx .^ 2 + yy .^ 2) / (2 * Smooth ^ 2));
    Gy = yy .* exp(-(xx .^ 2 + yy .^ 2) / (2 * Smooth ^ 2));

    Ix = conv2(F1, Gx, 'same');
    Iy = conv2(F1, Gy, 'same');
    It = F2-F1;

    U = zeros(size(F1,1),size(F1,2));
    V = zeros(size(F1,1),size(F1,2));
  
    for i = 1:size(F1,1)
        for j = 1:size(F1,2)
            A = [Ix(i,j), Iy(i,j)];
            b = It(i,j);
            G = A'*A;
            r = rank(G);

            if r == 2
                Ainv = pinv(A);
                U(i,j) = Ainv*b(1);
                V(i,j) = Ainv*b(2);
            else
                U(i,j) = 0;
                V(i,j) = 0;
            end  
        end
    end
end


