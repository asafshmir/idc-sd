function main()
   vr = VideoReader('C:\Users\Baruch\Documents\git\idc-sd\hw3\DATA\SLIDE.avi');
   
   nFrames = vr.NumberOfFrames;
   vr = VideoReader('C:\Users\Baruch\Documents\git\idc-sd\hw3\DATA\SLIDE.avi');
   vidHeight = vr.Height;
   vidWidth = vr.Width;
   
   seq = zeros(vidHeight,vidWidth,nFrames);
   
   k = 1;
   while hasFrame(vr)
       frame = rgb2gray(readFrame(vr));
       seq(:,:,k) = frame;
       k = k+1;
   end
   
   %imshow(seq(:,:,1),[]);
   disp('HI');
   
   threshold = 20;
   [B, CD] = segmentation_change_detection(seq,threshold);
   %imshow(CD(:,:,50),[]);
   imshow(B,[]);
   
    %imshow(seq(:,:,:,100));
    
%     im1 = zeros(100,100);
%     im1(20:30,20:30) = 200;
%     im1(70:80,70:80) = 200;
%     
%     im2 = zeros(100,100);
%     im2(22:32,21:31) = 200;
%     im2(78:88,85:95) = 200;
%    
%     im3=imread('C:\Users\Baruch\Documents\git\idc-sd\hw3\DATA\people\people2_1.jpg');
%     im3 = rgb2gray(im3);
%     im4=imread('C:\Users\Baruch\Documents\git\idc-sd\hw3\DATA\people\people2_2.jpg');
%     im4 = rgb2gray(im4);
%     
%     Smooth = 1;
%     Region = 5;
%     [U,V] = OF(im1, im2, Smooth, Region);
%     
%     showQuiver(im1,U,V);
 
    %testOFDemo();
    
    
%     testOFPeople();
%     testOFSlide();
%     writeVid();
end

function testOFDemo() 
 im1 = zeros(100,100);
    im1(20:30,20:30) = 200;
    im1(50:60,50:60) = 200;
    
    im2 = zeros(100,100);
    im2(22:32,21:31) = 200;
    im2(58:68,58:68) = 200;
    
    Smooth = 1;
    Region = 5;
    
    [U,V] = OF(im1, im2, Smooth, Region);
    
    showQuiver(im1,U,V,Region);

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
