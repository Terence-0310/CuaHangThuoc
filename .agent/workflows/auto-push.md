---
description: Auto push to git after successful compile
---

# Auto Push After Compile

After every successful `mvn compile` or `mvn clean compile`, automatically push code to git on branch `develop`.

## Steps

// turbo-all

1. Stage all changes
```
git add -A
```

2. Commit with a descriptive message summarizing the changes made
```
git commit -m "<descriptive message>"
```

3. Push to develop branch
```
git push origin develop
```

## Notes
- Branch: `develop`
- Remote: `origin`
- Always use descriptive commit messages summarizing what was changed
- Only push AFTER confirming BUILD SUCCESS from Maven
