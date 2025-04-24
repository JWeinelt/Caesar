# 🏛️ Caesar Backend
![Version](https://img.shields.io/badge/Version-v1.0.1-green)[![Java CI with Maven](https://github.com/JWeinelt/Caesar/actions/workflows/maven.yml/badge.svg)](https://github.com/JWeinelt/Caesar/actions/workflows/maven.yml)

**Caesar** is the central backend-component for managing Minecraft server networks - especially for [CloudNet v4](https://cloudnetservice.eu/). Caesar is a fast, secure and flexible application for modern Minecraft infrastructures.

> **DISCLAIMER**: Caesar is not web-based. You have to download the [Caesar Client](#) to work with the backend system.

---

## ✨ Features

- 🗺️ Management of servers integrated via CloudNET v4 or individually configured.
- 📈 Performance metrics and analytics
- 🔒 Secure user authentication with configurable permissions
- 📂 Access to file systems, consoles and logs
- 🎮 Player management with support system (Discord)
- 🔔 Notifications about critical stops and more
- 💬 Integrated secure chat for authenticated users
- 🔊 Voice chat for authenticated users
- 🎨 Configurable corporate designs for clients
- 🖇️ Integration for various Minecraft plugins

## 💻Are you looking for plugins?
Head to the [Caesar Marketplace!](https://market.caesarnet.cloud)

---

## ⚙️ Installation

### Requirements

- Java 21 or newer
- At least 1GB storage (may be more if your data gets larger)
- A database (MySQL or MSSQL) [See a list of required database permissions.](https://github.com/JWeinelt/Caesar/wiki/Database)
- When using Linux: ``wget``, ``unzip``, ``screen``

### Getting started
1. 📥 Download the latest .zip file from [Releases](https://github.com/JWeinelt/Caesar/releases)
2. 📂Extract it into the folder you want to use
3. 🔓On Unix-like systems, you **must** make start.sh executable:
	- `chmod +x start.sh`
4. 🚗Run the file `start.sh` (Linux) or `start.bat` (Windows)
5. 💬Follow the instructions on the screen.

Visit the [Full Caesar Guide](https://github.com/JWeinelt/Caesar/wiki) to get full instructions on how to install the backend and frontend.
    


## 🔌 API

Caesar provides a full HTTP-based REST API.

- 📚 Visit the [Developer Docs](https://github.com/JWeinelt/Caesar/wiki/API) for authentication, endpoints, examples and SDKs.

    

----------

## 🧪 Contributing

Contributions are welcome! Just clone this repository (for backend modifications):

```bash
git clone https://github.com/JWeinelt/Caesar.git
```
Please create a pull request for any contributions and use the [Code Conventions](https://github.com/JWeinelt/Caesar/wiki/Developer-Conventions).

If you find a problem with Caesar, please open an issue. But report any security issues using the ticket system on [Discord](https://dc.caesarnet.cloud).

----------

## 🤝 License

This project is licensed under  [GNU GPL v3 License](https://github.com/JWeinelt/Caesar?tab=GPL-3.0-1-ov-file).

----------

## ☕ Support & donations

If you want to help me maintaining this amazing project, please consider buying one of the [Service Plans](https://caesarnet.cloud/#pricing) or donating:

<a href='https://ko-fi.com/R5R41DYA9C' target='_blank'><img height='36' style='border:0px;height:36px;' src='https://storage.ko-fi.com/cdn/kofi6.png?v=6' border='0' alt='Buy Me a Coffee at ko-fi.com' /></a>

Please write me your GitHub username, so I'm able to add you as a sponsor to this page.

## 🧭 Road map

- [ ] Installing plugins via panel
- [ ] Update system
- [ ] Plugin market integration   
- [ ] Advanced permission system (including roles)
- [ ] Publish Caesar Android App on Google Play
- [ ] Docker Support
---

> **Caesar** – bring organization into your servers.

## ❤️ These amazing people make Caesar big!

<a href="https://github.com/FirecraftGHG"><img src="https://github.com/FirecraftGHG.png" width="50" height="50" alt="@FirecraftGHG"/></a>
<a href="https://github.com/PhastixTV"><img src="https://github.com/PhastixTV.png" width="50" height="50" alt="@PhastixTV"/></a>
<a href="https://github.com/LeMichiii"><img src="https://github.com/LeMichiii.png" width="50" height="50" alt="@LeMichiii"/></a>
