donext == 1 {donext = 0; r=substr($1,2); u = $3; ymd = $5; hms = $6; 
file="SvnStats/" r ".lines";
lines="";
getline lines < file;
close(file);
remindex = index(lines, " removed")
added = substr(lines, 7, remindex-7)
removed = substr(lines, remindex+8)
sub(/ /, "", added)
sub(/ /, "", removed)
printf("%s\t%s\t%sT%s\t%s\t%s\n", r, u, ymd, hms, added, removed);
}
$0=="------------------------------------------------------------------------" { donext=1; }
