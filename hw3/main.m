function main()
 
%     [U, V] = testOFDemo(true);
%     testOFPeople();
%     testOFSlide();
%     segsOfSize();
      segOfDirection();

end

function segOfDirection()
    [U, V] = testOFDemo(false);
    segs = seg_OF_direction(U,V,[1 20 40]);
    figure
    imshow(segs,[]);
    
end
function segsOfSize() 
    [U, V] = testOFDemo(false);
    [background, foreground] = seg_OF_size(U,V,0.01);
    figure;
    imshow(background,[1 256]);
    imshow(foreground,[1 256]);
end

function [U, V] = testOFDemo(shouldPlot) 
    im1 = zeros(100,100);
    im1(20:30,20:30) = 200;
    im1(50:60,50:60) = 200;
    
    im2 = zeros(100,100);
    im2(22:32,21:31) = 200;
    im2(58:68,58:68) = 200;
    
    Smooth = 1;
    Region = 5;
    
    [U,V] = OF(im1, im2, Smooth, Region);
    if shouldPlot == true
        showQuiver(im1,U,V,Region);
    end

end

function testOFSlide() 
   
    MOV = VideoReader('./SLIDE.avi');
    seq = readframe(MOV);
    im1 = rgb2gray(seq(:,:,:,1));
    im2 = rgb2gray(seq(:,:,:,2));

    [U,V] = OF(im1, im2, Smooth, Region);

    showQuiver(im1,U,V,Region);
end

function testOFPeople()
    Smooth = 1;
    Region = 5;
       
    im1 = rgb2gray(imread('people/people2_1.jpg')); 
    im2 = rgb2gray(imread('people/people2_2.jpg')); 
   
    [U,V] = OF(im1, im2, Smooth, Region);

    showQuiver(im1,U,V,Region);

end

function writeVid() 
for i=1:10 
    seq3(:,:,:,i)=imread(sprintf('people/people2_%d.jpg',i),'jpg'); 
end;
vidObj = VideoWriter('people.avi');
open(vidObj);
for i=1:10
    writeVideo(vidObj,seq3(:,:,:,i)); 
end
close(vidObj);
end

function result = findResult(A,th) 
%     result = find(A<9 & ~mod(A,2) & A~=2)
    result = find(A>th);
end

function showQuiver(im1, u, v, region)
    [X,Y]=meshgrid(1:size(im1,2),1:size(im1,1)); 
    nu12=medfilt2(u,[region region]);
    nv12=medfilt2(v,[region region]);
    figure; 
    imshow(im1,[]);
    hold on;
    quiver(X(1:region:end,1:region:end), ...
           Y(1:region:end,1:region:end), ...
           nu12(1:region:end,1:region:end), ...
           nv12(1:region:end,1:region:end),region);
end