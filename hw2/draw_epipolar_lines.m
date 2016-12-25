function draw_epipolar_lines = draw_epipolar_lines(im1,im2,F,p,f1,f2)
    P = [p(1) p(2) 1]';
    
    figure(f1);
    plot(p(1),p(2),'*y');
    
    l = F*P;
    figure(f2);
    plot(l(1),l(2),'*y');
    
    points = lineToBorderPoints(l',size(im1)); 
    line(points(:,[1,3])',points(:,[2,4])');
    
    hold on
end