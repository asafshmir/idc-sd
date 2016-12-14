function draw_epipolar_lines = draw_epipolar_lines(im1,im2,F,p,f1,f2)
    P = [p(1) p(2) 1];
    P = P';
    l1 = F*P;
    figure(f1);
    plot(l1(1),l1(2),'*r');
    
    l2 = F'*P;
    figure(f2);
    plot(l2(1),l2(2),'*r');
end