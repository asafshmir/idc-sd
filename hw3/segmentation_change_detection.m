function [B, CD] = segmentation_change_detection(seq,th)
    % Compute the background (median of all frames)
    B = uint8(median(seq, 3));
    
    % Detect changes (differences larger than the threshold)
    CD = seq(:,:,:);
    for f = 1:size(CD,3)
        for i = 1:size(CD,1)
            for j = 1:size(CD,2)
            
                if(abs(CD(i,j,f) - B(i,j)) > th)
                    CD(i,j,f) = 1;
                else
                    CD(i,j,f) = 0;
                end
                
            end
        end
    end
    
end
% 
% function main()
%    vr = VideoReader('C:\Users\Baruch\Documents\git\idc-sd\hw3\DATA\SLIDE.avi');
%    
%    nFrames = vr.NumberOfFrames;
%    vr = VideoReader('C:\Users\Baruch\Documents\git\idc-sd\hw3\DATA\SLIDE.avi');
%    vidHeight = vr.Height;
%    vidWidth = vr.Width;
%    
%    seq = zeros(vidHeight,vidWidth,nFrames);
%    
%    k = 1;
%    while hasFrame(vr)
%        frame = rgb2gray(readFrame(vr));
%        seq(:,:,k) = frame;
%        k = k+1;
%    end
%    
%    %imshow(seq(:,:,1),[]);
%    disp('HI');
%    
%    threshold = 20;
%    [B, CD] = segmentation_change_detection(seq,threshold);
%    %imshow(CD(:,:,50),[]);
%    imshow(B,[]);
% end