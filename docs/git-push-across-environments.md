# Git Push Fails When Moving Between Environments

## The Problem

You clone a repo on Machine A, do your work, then copy the entire project folder
to Machine B (or a different user account). When you try `git push`, it fails with
errors like:

```
fatal: could not read Username for 'https://github.com': Device not configured
```
```
git@github.com: Permission denied (publickey).
```
```
ssh: connect to host github.com port 22: Bad file descriptor
```

## Why This Happens

Git authentication is tied to the **machine and user account**, not the repository
folder. When you copy a repo to a new environment, the code and commit history come
along perfectly — but the credentials do not.

### HTTPS remotes

If the remote URL looks like `https://github.com/user/repo.git`, Git relies on a
**credential helper** (macOS Keychain, Windows Credential Manager, `git-credential-store`,
etc.) to supply your username and token. That credential helper stores secrets in the
OS-level keychain of the original machine. The new machine has no entry for
`github.com`, so Git can't authenticate.

### SSH remotes

If the remote URL looks like `git@github.com:user/repo.git`, Git uses your
**SSH private key** (typically `~/.ssh/id_ed25519` or `~/.ssh/id_rsa`). Two things
can go wrong on a new machine:

1. **No matching key** — the new machine has a different SSH key (or one meant for a
   different service like GitLab), so GitHub rejects it.
2. **Port blocked** — shared networks often block port 22 (SSH). The connection
   times out or returns "Bad file descriptor."

### Shared proxies

Many shared environments route traffic through an HTTP proxy. Git picks this up
from environment variables (`HTTP_PROXY`, `HTTPS_PROXY`) or `~/.gitconfig`. If the
proxy doesn't allow connections to `github.com`, or if the proxy config from the old
machine doesn't apply, the push fails with proxy resolution errors.

## How to Fix It

### Option 1: GitHub CLI (recommended, easiest)

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

1. Go to https://github.com/settings/tokens
2. Generate a **fine-grained** or **classic** token with `repo` scope
3. Push and paste the token when prompted:

```bash
git remote set-url origin https://github.com/USER/REPO.git
git push
# Username: your-github-username
# Password: <paste token>
```

To cache it so you don't re-enter every time:

```bash
git config --global credential.helper store   # plaintext file
# or on macOS:
git config --global credential.helper osxkeychain
```

### Option 3: SSH key

1. Generate a key (if you don't have one for GitHub):
   ```bash
   ssh-keygen -t ed25519 -C "your-email@example.com"
   ```
2. Add the public key to https://github.com/settings/keys
3. Switch the remote to SSH:
   ```bash
   git remote set-url origin git@github.com:USER/REPO.git
   ```
4. If port 22 is blocked (shared firewall), use GitHub's port 443 fallback.
   Add to `~/.ssh/config`:
   ```
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

**Commits are local and safe.** A failed push doesn't lose any work. The repo,
its history, and all your commits are intact in the folder. You just need to set up
authentication on the new machine, then `git push` will send everything up.
