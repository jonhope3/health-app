# Git Push Fails When Moving Between Environments

## The Problem

You clone a repo on Machine A, do your work, then copy the
project folder to Machine B. When you try `git push`, it fails:

```text
fatal: could not read Username for 'https://github.com'
```

```text
git@github.com: Permission denied (publickey).
```

```text
ssh: connect to host github.com port 22: Bad file descriptor
```

## Why This Happens

Git authentication is tied to the **machine and user account**,
not the repository folder. The code and commit history copy fine
but the credentials do not.

### HTTPS remotes

If the remote URL is `https://github.com/user/repo.git`, Git
relies on a **credential helper** (macOS Keychain, etc.) to
supply your username and token. The new machine has no keychain
entry for `github.com`, so Git can't authenticate.

### SSH remotes

If the remote URL is `git@github.com:user/repo.git`, Git uses
your **SSH private key** (`~/.ssh/id_ed25519`). On a new
machine:

1. **No matching key** — different or missing SSH key
2. **Port blocked** — shared networks often block port 22

### Shared proxies

Shared environments may route traffic through an HTTP proxy.
If the proxy doesn't allow `github.com`, or the old machine's
proxy config doesn't apply, the push fails.

## How to Fix It

### Option 1: GitHub CLI (recommended)

```bash
# Install (macOS)
brew install gh

# Authenticate — opens a browser flow
gh auth login

# Push normally
git push
```

This sets up a credential helper automatically.

### Option 2: Personal Access Token (HTTPS)

1. Go to <https://github.com/settings/tokens>
2. Generate a token with `repo` scope
3. Push and paste the token when prompted:

```bash
git remote set-url origin https://github.com/USER/REPO.git
git push
# Username: your-github-username
# Password: <paste token>
```

To cache it so you don't re-enter every time:

```bash
git config --global credential.helper store
# or on macOS:
git config --global credential.helper osxkeychain
```

### Option 3: SSH key

1. Generate a key (if needed):

   ```bash
   ssh-keygen -t ed25519 -C "your-email@example.com"
   ```

2. Add the public key at <https://github.com/settings/keys>
3. Switch the remote to SSH:

   ```bash
   git remote set-url origin git@github.com:USER/REPO.git
   ```

4. If port 22 is blocked, use GitHub's port 443 fallback.
   Add to `~/.ssh/config`:

   ```text
   Host github.com
       Hostname ssh.github.com
       Port 443
       User git
   ```

### Bypassing a proxy (temporary)

If your machine has proxy env vars that block GitHub:

```bash
NO_PROXY="*" git push
# or
git -c http.proxy= -c https.proxy= push
```

## Key Takeaway

**Commits are local and safe.** A failed push doesn't lose
any work. The repo, its history, and all commits are intact.
You just need to set up authentication on the new machine,
then `git push` will send everything up.
