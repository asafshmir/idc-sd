% Plot lines between two sets of matching point on a given images
function plot_correspondence(im1, im2, Lc)

    points1 = Lc(:,1:2)';
    points2 = Lc(:,3:4)';

    figure;
    [rows cols colors] = size(im1);
    
    newIm = uint8(zeros(rows, 2*cols, colors));
    newIm(:, 1:cols, :) = im1;
    newIm(:, (cols+1):end, :) = im2;
    imshow(newIm);
    hold on;
    
    % Plot the given points
    plot(points1(1,:), points1(2,:), '*r');
    plot(points2(1,:)+cols, points2(2,:), '*r');
    
    % For each pair of points, draw a line between them
    for i = 1:length(points1(1,:))
        line([points1(1,i) points2(1,i)+cols],[points1(2,i) points2(2,i)], 'Color', 'c');
    end
end