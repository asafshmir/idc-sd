function p = proj(P,M)
    a = [(M(1,1)*P(1)/P(3))+M(1,3),(M(2,2)*P(2)/P(3))+M(2,3),1];
    p = a';
end
