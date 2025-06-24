# 🤝 Contributing to Caesar

Hello, and thank you for considering contributing to **Caesar** — a project born from passion, structure, and the pursuit of digital elegance. We welcome contributions of all kinds: bug fixes, new features, documentation improvements, ideas, and feedback.

Please take a moment to review this guide before submitting pull requests or issues.

---

## 📦 How to Contribute

1. **Fork the repository**
2. **Create a new branch**  
   Use a clear and descriptive branch name:  
   `git checkout -b feat/implement-sandbox-mode`
3. **Write clean, understandable code**
4. **Test your changes** locally before pushing.
5. **Commit using the Conventional Commits format** (see below).
6. **Push to your fork** and submit a pull request.
7. Wait for review and be open to constructive feedback.

---

## 🧠 Code Style

- Stick to the existing architecture and naming conventions.
- Keep methods small and focused.
- Avoid unnecessary dependencies.
- Write comments where clarity might suffer.
- Prefer composition over inheritance where possible.

---

## 🧪 Testing

- Add tests for new features or behaviors.
- Make sure existing tests **still pass**.
- If you're unsure how to test something, feel free to ask in the pull request.

---

## 🔖 Commit Message Format (Conventional Commits)

We use the [Conventional Commits](https://www.conventionalcommits.org) specification for all commit messages. This makes it easier to automate changelogs, releases, and track changes over time.

### 🔤 Format

`[type](optional-scope): short summary`

**Example:**

`feat(core): add sandbox mode for new users`
`fix(database): prevent crash on empty queries`
`docs(readme): add setup instructions`

### ✅ Common Types

| Type       | Description                                    |
|------------|------------------------------------------------|
| `feat`     | A new feature                                  |
| `fix`      | A bug fix                                      |
| `docs`     | Documentation-only changes                     |
| `style`    | Formatting, white-space, etc. (no code logic)  |
| `refactor` | Code change that neither fixes nor adds a feature |
| `perf`     | Performance improvement                        |
| `test`     | Adding or updating tests                       |
| `build`    | Changes to build system, dependencies, etc.    |
| `ci`       | Continuous integration related changes         |
| `chore`    | Maintenance that doesn’t change code behavior  |

### 💥 Breaking Changes

For changes that break backward compatibility, add a footer like this:

`BREAKING CHANGE: configuration files from version 1.0 are no longer supported`


---

## 📬 Feedback and Questions

If you’re not ready to code yet but want to help — open an issue, suggest ideas, or report bugs.

We believe in constructive dialogue, thoughtful contributions, and the open-source spirit.  
Let’s build something excellent — together.

---

Thank you for being part of the Caesar project 🙏  
— The Caesar Team
