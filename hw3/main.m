function main()
    MOV = VideoReader('./SLIDE.avi');
    seq = read(MOV);
    imshow(seq(:,:,:,100));
  
%     writeVid();
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

function showQuiver(im1, u, v)
    [X,Y]=meshgrid(1:size(im1,2),1:size(im1,1)); 
    nu12=medfilt2(u,[5 5]);
    nv12=medfilt2(v,[5 5]);
    figure; 
    imshow(im1,[]);
    hold on;
    quiver(X(1:5:end,1:5:end), ...
           Y(1:5:end,1:5:end), ...
           nu12(1:5:end,1:5:end), ...
           nv12(1:5:end,1:5:end),5);
end