BEGIN {
 file="DontCount.names";
 while (getline name < file) {
   names[name] = 1;
 }
}
{if (! names[$1]) print $1;}
