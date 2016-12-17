function writeImage(img,fileName,fmt);
% 
% writeImage(img,fileName);
%
% Writes a grayscale image (stored in a matrix) onto a file.
% Stores the image in TIF format.
% img - is a grayscale image with values [0..255]
% fileName is a string
%
% fmt - is an optional argument with the format of the image.
%       see fmt of imread.
%
% Written by Hagit Hel-Or  I.D. xxxxxxxx
% Last Modified 14/12/98
%
 
if nargin <2
    error('USAGE: writeImage(img,fileName)');
end;

if nargin <3
    fmt='tif'; end;

img = uint8(img); % imwrite expects uint8
ext_name=[fileName '.' fmt];
imwrite(img,ext_name,fmt);



