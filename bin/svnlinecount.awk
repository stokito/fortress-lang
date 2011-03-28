/^[+][^+]/ {pluslines++;}
/^[-][^-]/ {minuslines++;}
END {print "added", pluslines, "removed", minuslines; }
