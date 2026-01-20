cd "C:\CMeaningOS\CMeaningOSMeaningOSApp"
git add -A
git diff --cached --quiet
if ($LASTEXITCODE -ne 0) {
    git commit -m "auto-checkpoint"
}
