function [U,V] = OF(F1, F2, Smooth, Region)
    %OF Summary of this function goes here
    %   Detailed explanation goes here
    [xx, yy] = meshgrid(-Region:Region, -Region:Region);

    Gx = xx .* exp(-(xx .^ 2 + yy .^ 2) / (2 * Smooth ^ 2));
    Gy = yy .* exp(-(xx .^ 2 + yy .^ 2) / (2 * Smooth ^ 2));

    Ix = conv2(double(F1), Gx, 'same');
    Iy = conv2(double(F1), Gy, 'same');
    It = double(F2-F1);

    U = zeros(size(F1,1),size(F1,2));
    V = zeros(size(F1,1),size(F1,2));
    
    XWindow = -Region:Region;
    YWindow = -Region:Region;
    
    for i = 1+Region:size(F1, 1)-Region
        for j = 1+Region:size(F1, 2)-Region

            b = -It(i+XWindow, j+YWindow);
            msize = size(b,1)*size(b,2);
            b = reshape(b,msize,1);
            Ixnew = Ix(i+XWindow, j+YWindow);
            Iynew = Iy(i+XWindow, j+YWindow);

            Ixnew = reshape(Ixnew,msize,1);
            Iynew = reshape(Iynew,msize,1);
            A = [Ixnew Iynew];

            G = A'*A;
            if rank(G) >= 2
                Ainv = pinv(A);
                result = Ainv * b;
                U(i,j) = result(1);
                V(i,j) = result(2);
            else
                U(i,j) = 0;
                V(i,j) = 0;  
            end

        end
    end
end


