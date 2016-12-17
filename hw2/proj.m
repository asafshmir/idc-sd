function p = proj(P,M)
    a = M*[P',1]' ./ P(3);
    p = a';
end
