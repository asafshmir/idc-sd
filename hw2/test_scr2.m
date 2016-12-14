%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%The [rameters of the left and right cameras 
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

       
% Image 1: Intrinsic parameters: 
    
  %Image center: 
     ox1 = 331.023000; 
     oy1 = 259.386377; 
     
  %Scale factor: 
     Sx1 = 1100.504780; 
     Sy1 = 1097.763735;
     
%Focal length: 
     f1=1.0;
     
% Image 1: extrinsic parameters: 
     %Rotation: 
     R1=[ 1  0 0 
         0 1 0
         0 0 1];
    
%Translation:
    T1=[0 0 0]';% Image 1: Intrinsic parameters: 
    
     
% Image 2: Intrinsic parameters: 
    %Image center: 
    ox2 = 320.798101;
    oy2 = 236.431326;

    % Scale factor: 
    Sx2 = 1095.671499; 
    Sy2 = 1094.559584;
   
    % Focal length: 
    f2=1.0;
     
    T2=-[178.2218
        18.8171
        -13.7744];
    
    R2 =[
         0.9891    0.0602   -0.1346
        -0.0590    0.9982    0.0134
         0.1351   -0.0053    0.9908];

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% Compute intrinsic projection matrices: Mint1 and Mint2
    Mint1 = [
            Sx1*f1  0       ox1
            0       Sy1*f1  oy1
            0       0       1];

    Mint2 = [
            Sx2*f2  0       ox2
            0       Sy2*f2  oy2
            0       0       1];     
        
% Compute the projection matrices
    M1 = Mint1*[R1 -R1*T1];
    M2 = Mint2*[R2 -R2*T2];
    
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% (4) Verify that the null space of M is indeed the COP     %
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
cop1 = null(M1);
cop2 = null(M2);

% These should be zero vectors (or almost zero)
test1 = M1*cop1;
test2 = M2*cop2;

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% (6) Projection                                             %
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
 
   % Write a finction p=proj(P,M) that recieves as input 
   % the 3D point P,  and a projection  matrix M, and output 
   % the  2D coordinates of the projected point.
    P=[-140,50,1200]';
    p1=proj(P,M1);
    p2=proj(P,M2);
    
    Q=[30,100,2000]'; 
    q1=proj(Q,M1);
    q2=proj(Q,M2);

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% Read the images into im1 and im2,  and dispaly them.      %
% Using the command figure(f1) will redisplay the figure    %
% containing im1.                                           %
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

 
    im1=readImage('Left.tif');
    im2=readImage('Right.tif');

    figure;
    imshow(im1,[])
    hold on;
    f1=gcf;
    
    figure;
    imshow(im2,[])
    hold on;
    f2=gcf;

%     display the projection on the two images
    figure(f1);
%     plot(p1(1),p1(2),'*r');
    
    figure(f2);
%     plot(p2(1),p2(2),'*r');
    
    figure;
    imshow(im1,[])
    hold on;
    f3=gcf;
    
    figure;
    imshow(im2,[])
    hold on;
    f4=gcf;
    
    figure(f3);
%     plot(q1(1),q1(2),'*r');
    
    figure(f4);
%     plot(q2(1),q2(2),'*r');
     
    
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% (7-10) compute the fundamnral matrix F, the epipoles el   % 
% and er, and Fel erF                                       %
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

    el = M2*cop1;
    er = M1*cop2;

    M1_trans = M1'*inv((M1*(M1')));
    M2_trans = M2'*inv((M2*(M2')));

    %       P = proj(cop2,M1);

%     P2 = zeros(1,4);
%     P2(1:3) = P;
%     P2(4) = 1;

    ert = M1*cop2;
    erx = [
            0       -ert(3) ert(2)
            ert(3)  0       -ert(1)
            -ert(2) ert(1)  0];

    F = erx*M1*M2_trans;
      
    Fel = F*el;
    erF = er'*F;
      
    % Please normalize F by F(3,3).
    F = F.*(1/F(3,3));
 
    % Check that the computed epipoles are consistent with F
    testl = F*el;
    testr = er'*F;
   
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% (11) Draw epipolar lines                           
% You have to write the function
% [f1,f2]=draw_epipolar_lines(im1,im2,F,p,f1,f2) (f1 and f2 are the figures on
% which im1 and im2 are already dispalyed, the value of f1 and f2 are 0 if the images
% are note displayed yet. In this case, the f1 and f2 are the output of
% the function for later use of the figures
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

    p=[283,348];% the selected point
%     testp = F*p;
    
    draw_epipolar_lines(im1,im2,F,p,f1,f2)

    figure(f1);

     % Choose points from image 1 (look at help getpts)
%     figure(f1);
%     [Px,Py]=getpts
%     
%     % Display the  set of epipolar lines which corresponds to the chosen points
%     for i=1:length(Px)
%         draw_epipolar_lines(im1,im2,F,[Px(i),Py(i)]',f1,f2)
%     end

%     % (12) Corner detector + matching removal by Sampson distance
    ps1 = [q1(1:2)'; q2(1:2)'];
    ps2 = [q2(1:2)';q1(1:2)'];
    P = stereo_list(ps1,ps2,M1,M2);

    disp(P);
 
