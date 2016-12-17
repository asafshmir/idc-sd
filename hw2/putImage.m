function putImage(img,fgn)
% 
% putImage(img)
%
% Displays a grayscale image in a resizable
% window with x&y axes. Used to view small images.
% img - is a grayscale image with values [0..255]
%
% fgn - an optional argument defining the  figure number on 
%                   which the image is displayed.
%
% Written by Hagit Hel-Or  I.D. xxxxxxxx
% Last Modified 5/3/97
%

if nargin <1
    error('USAGE: putImage(img)');
end;


if nargin <2
    figure;
 else 
    figure(fgn);
end;

img = img + 1;
image(img); 
colormap(gray(256));

