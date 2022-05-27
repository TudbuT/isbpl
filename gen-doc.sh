isbpldoc $1.isbpl > $1.md.tmp
git merge-file docs/$1.md docs/base/$1.md $1.md.tmp || echo Manual merge needed!
mv $1.md.tmp docs/base/$1.md
