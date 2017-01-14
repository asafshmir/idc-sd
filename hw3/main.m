function main()
    video = 'C:\Users\Baruch\Documents\git\idc-sd\hw3\DATA\SLIDE.avi';
    images = 'C:\Users\Baruch\Documents\git\idc-sd\hw3\DATA\people\people2_';
    Smooth = 1;
    Region = 5;

    % Question 3
    %testOFDemo(Smooth, Region);
    
    % Questions 4-8
    %testOF(video, images, Smooth, Region);
         
    %testSegmentationChangeDetection(video);
    
    %     segsOfSize();
%       segOfDirection();

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

function [U, V] = testOFDemo(Smooth, Region) 
    im1 = zeros(200,200);
    im1(20:30,20:30) = 200;
    im1(50:60,50:60) = 200;
    im1(100:130,100:130) = 200;  
    
    im2 = zeros(200,200);
    im2(22:32,21:31) = 200;
    im2(58:68,58:68) = 200;
    im2(150:180,150:180) = 200;  
        
    [U,V] = OF(im1, im2, Smooth, Region);
    showQuiver(im1,U,V,Region);
    
end

function testOF(video, images, Smooth, Region) 
    
    % 5(a) - several pairs of frames from each of the sequences
    % slide video
    seq = video2grey_seq(video);

    im1 = seq(:,:,1);
    im2 = seq(:,:,2);
    [U,V] = OF(im1, im2, Smooth, Region);
    showQuiver(im1,U,V,Region);
    
    im1 = seq(:,:,1);
    im2 = seq(:,:,100);
    [U,V] = OF(im1, im2, Smooth, Region);
    showQuiver(im1,U,V,Region);    
   
    % people images
    im1 = rgb2gray(imread(strcat(images,'1.jpg')));
    im2 = rgb2gray(imread(strcat(images,'2.jpg')));
    [U,V] = OF(im1, im2, Smooth, Region);
    showQuiver(im1,U,V,Region);
    
    im1 = rgb2gray(imread(strcat(images,'1.jpg')));
    im2 = rgb2gray(imread(strcat(images,'30.jpg')));
    [U,V] = OF(im1, im2, Smooth, Region);
    showQuiver(im1,U,V,Region);
    
    im1 = rgb2gray(imread(strcat(images,'1.jpg')));
    im2 = rgb2gray(imread(strcat(images,'9.jpg')));
    [U,V] = OF(im1, im2, Smooth, Region);
    showQuiver(im1,U,V,Region);
    
    im1 = rgb2gray(imread(strcat(images,'9.jpg')));
    im2 = rgb2gray(imread(strcat(images,'17.jpg')));
    [U,V] = OF(im1, im2, Smooth, Region);
    showQuiver(im1,U,V,Region);
    
    im1 = rgb2gray(imread(strcat(images,'17.jpg')));
    im2 = rgb2gray(imread(strcat(images,'26.jpg')));
    [U,V] = OF(im1, im2, Smooth, Region);
    showQuiver(im1,U,V,Region);
    
    % 5(b) - Play with the algorithm parameters
    im1 = rgb2gray(imread(strcat(images,'9.jpg')));
    im2 = rgb2gray(imread(strcat(images,'17.jpg')));
    [U,V] = OF(im1, im2, Smooth, Region);
    showQuiver(im1,U,V,Region);

    im1 = rgb2gray(imread(strcat(images,'9.jpg')));
    im2 = rgb2gray(imread(strcat(images,'17.jpg')));
    [U,V] = OF(im1, im2, 20, Region);
    showQuiver(im1,U,V,Region);

    im1 = rgb2gray(imread(strcat(images,'9.jpg')));
    im2 = rgb2gray(imread(strcat(images,'17.jpg')));
    [U,V] = OF(im1, im2, Smooth, 30);
    showQuiver(im1,U,V,Region);
    
    % 5(c) - Play with the distance between the pair of frames
    seq = video2grey_seq(video);
    jump = 10;
    start = 1;
    frames = 5;
    
    for i=1:frames
        im1 = seq(:,:,start);
        im2 = seq(:,:,start+jump);
        [U,V] = OF(im1, im2, Smooth, Region);
        showQuiver(im1,U,V,Region);
        start = start + jump;
    end

    % 7 - resize the image to see if you obtain different optical flow for different scales
    seq = video2grey_seq(video);
 
    im1 = seq(:,:,40);
    im2 = seq(:,:,50);
    [U,V] = OF(im1, im2, Smooth, Region);
    showQuiver(im1,U,V,Region);

    im1 = imresize(seq(:,:,40), 1.5);
    im2 = imresize(seq(:,:,50), 1.5);
    [U,V] = OF(im1, im2, Smooth, Region);
    showQuiver(im1,U,V,Region);

    im1 = imresize(seq(:,:,40), 0.5);
    im2 = imresize(seq(:,:,50), 0.5);
    [U,V] = OF(im1, im2, Smooth, Region);
    showQuiver(im1,U,V,Region);

    im1 = imresize(seq(:,:,40), 0.25);
    im2 = imresize(seq(:,:,50), 0.25);
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

function seq = video2grey_seq(video)
   
   vr = VideoReader(video);
   
   nFrames = vr.NumberOfFrames;
   vr = VideoReader(video);
   vidHeight = vr.Height;
   vidWidth = vr.Width;
   
   seq = zeros(vidHeight,vidWidth,nFrames);
   
   k = 1;
   while hasFrame(vr)
       frame = rgb2gray(readFrame(vr));
       seq(:,:,k) = frame;
       k = k+1;
   end
end

function testSegmentationChangeDetection(video) 
    
   seq = video2grey_seq(video); 

   threshold = 20;
   [B, CD] = segmentation_change_detection(seq,threshold);
   %imshow(CD(:,:,50),[]);
   imshow(B,[]);
   
end